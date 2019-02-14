package nu.nerd.beastmaster;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import net.sothatsit.blockstore.BlockStoreApi;
import nu.nerd.beastmaster.commands.BeastItemExecutor;
import nu.nerd.beastmaster.commands.BeastLootExecutor;
import nu.nerd.beastmaster.commands.BeastMasterExecutor;
import nu.nerd.beastmaster.commands.BeastMobExecutor;
import nu.nerd.beastmaster.commands.BeastObjectiveExecutor;
import nu.nerd.beastmaster.commands.BeastZoneExecutor;
import nu.nerd.beastmaster.commands.ExecutorBase;
import nu.nerd.beastmaster.mobs.MobProperty;
import nu.nerd.beastmaster.mobs.MobType;
import nu.nerd.beastmaster.mobs.MobTypeManager;
import nu.nerd.beastmaster.objectives.Objective;
import nu.nerd.beastmaster.objectives.ObjectiveManager;
import nu.nerd.beastmaster.objectives.ObjectiveTypeManager;
import nu.nerd.beastmaster.zones.Zone;
import nu.nerd.beastmaster.zones.ZoneManager;
import nu.nerd.entitymeta.EntityMeta;

// ----------------------------------------------------------------------------
/**
 * Plugin, command handling and event handler class.
 */
public class BeastMaster extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * Configuration wrapper instance.
     */
    public static final Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static BeastMaster PLUGIN;

    /**
     * Zone manager as a singleton.
     */
    public static final ZoneManager ZONES = new ZoneManager();

    /**
     * Item manager as a singleton.
     */
    public static final ItemManager ITEMS = new ItemManager();

    /**
     * Mob type manager as a singleton.
     */
    public static final MobTypeManager MOBS = new MobTypeManager();

    /**
     * Loot table manager as a singleton.
     */
    public static final LootManager LOOTS = new LootManager();

    /**
     * Manages all objectives.
     */
    public static final ObjectiveManager OBJECTIVES = new ObjectiveManager();

    /**
     * Manages all objective type.
     */
    public static final ObjectiveTypeManager OBJECTIVE_TYPES = new ObjectiveTypeManager();

    /**
     * Metadata name (key) used to tag affected mobs.
     */
    public static final String MOB_META_KEY = "BM_Mob";

    /**
     * Shared metadata value for all affected mobs.
     */
    public static FixedMetadataValue MOB_META;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        MOB_META = new FixedMetadataValue(this, null);

        PLUGIN = this;
        saveDefaultConfig();
        CONFIG.reload();

        addCommandExecutor(new BeastMasterExecutor());
        addCommandExecutor(new BeastZoneExecutor());
        addCommandExecutor(new BeastItemExecutor());
        addCommandExecutor(new BeastLootExecutor());
        addCommandExecutor(new BeastMobExecutor());
        addCommandExecutor(new BeastObjectiveExecutor());

        getServer().getPluginManager().registerEvents(this, this);

        // Every tick, do particle effects for objectives.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                OBJECTIVES.tickAll();
            }
        }, 1, 1);

        OBJECTIVES.extractSchematics();
    } // onEnable

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        OBJECTIVES.removeAll();
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn a mob of the specified custom mob type.
     * 
     * @param loc the Location where the mob spawns.
     * @param mobType the custom mob type.
     * @param checkCanFit if true, the available space at the location is
     *        checked to see if it can accomodate the mob, and if not, the mob
     *        is removed.
     * @return the new LivingEntity, or null if it could not fit or an invalid
     *         type was specified.
     */
    public LivingEntity spawnMob(Location loc, MobType mobType, boolean checkCanFit) {
        MobProperty entityTypeProperty = mobType.getDerivedProperty("entity-type");
        EntityType entityType = (EntityType) entityTypeProperty.getValue();
        LivingEntity livingEntity = null;
        if (entityType == null) {
            getLogger().info("Mob type " + mobType.getId() + " cannot spawn at " + Util.formatLocation(loc) + ": no entity type.");
        } else {
            // When _spawningMobType is non-null, we know that custom
            // (plugin-generated) mob spawns originate from this plugin.
            // World.spawnEntity() calls into onCreatureSpawn().
            _spawningMobType = mobType;
            _spawningCheckCanFit = checkCanFit;
            livingEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);

            // Check if removed because it can't fit.
            if (!livingEntity.isValid()) {
                livingEntity = null;
            }
            _spawningMobType = null;
        }
        return livingEntity;
    }

    // ------------------------------------------------------------------------
    /**
     * When a world is loaded, ensure that a top-level zone for that world
     * exists.
     */
    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (ZONES.getZone(world.getName()) == null) {
            ZONES.addZone(new Zone(world.getName(), world));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a player breaks an objective block, do treasure drops and stop that
     * the particle effects.
     * 
     * Handle players breaking ore blocks by consulting the most specific loot
     * table for the applicable Zone/Condition and block type.
     * 
     * Don't drop special items for player-placed blocks.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        long start = System.nanoTime();
        boolean placed = BlockStoreApi.isPlaced(block);
        if (CONFIG.DEBUG_BLOCKSTORE) {
            float elapsedMs = (System.nanoTime() - start) * 1.0e-6f;
            if (elapsedMs > 10.0f) {
                getLogger().info("BlockStoreApi.isPlaced() took: " + String.format("%3.2f", elapsedMs));
            }
        }

        if (placed) {
            return;
        }

        handleBlockBreakCustomDrops(event, block);

        Objective objective = OBJECTIVES.getObjective(block);
        if (objective != null) {
            // Prevent the objective break from being logged by LogBlock.
            event.setCancelled(true);

            OBJECTIVES.removeObjective(objective);
            objective.spawnLoot(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a mob spawns, perform zone-appropriate replacement with custom mob
     * types.
     * 
     * Mobs that are not replaced are customised according to their EntityType.
     * 
     * All mobs that go through this process end up with persistent metadata
     * value "mob-type" set to the ID of their MobType. Note, however, that
     * CUSTOM spawns from other plugins will not have the "mob-type" metadata.
     * 
     * All mobs are tagged with their spawn reason as metadata. I would like to
     * tag slimes that spawn by splitting according to whether the original
     * slime came from a spawner, but there's no easy way to find the parent
     * slime.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        replaceNetherSkeletonSpawn(event);

        LivingEntity entity = event.getEntity();
        // Tag spawn reason. Replacement mobs will have SpawnReason.CUSTOM.
        EntityMeta.api().set(entity, this, "spawn-reason", event.getSpawnReason().toString());

        switch (event.getSpawnReason()) {
        case CUSTOM:
            // Plugin driven spawns.
            if (_spawningMobType != null) {
                if (!_spawningCheckCanFit || canFit(entity)) {
                    _spawningMobType.configureMob(entity);
                } else {
                    entity.remove();
                }
            }
            break;

        case DEFAULT:
        case NATURAL:
        case REINFORCEMENTS:
        case INFECTION:
        case VILLAGE_INVASION:
        case VILLAGE_DEFENSE:
        case EGG:
        case SPAWNER_EGG:
        case BUILD_SNOWMAN:
        case BUILD_IRONGOLEM:
        case BUILD_WITHER:
        case SILVERFISH_BLOCK:
        case ENDER_PEARL:
            // Vanilla spawns.
            Location loc = event.getLocation();
            Zone zone = ZONES.getZone(loc);
            if (zone != null) {
                DropSet replacement = zone.getMobReplacementDropSet(entity.getType());
                if (replacement != null) {
                    Drop drop = replacement.chooseOneDrop();
                    switch (drop.getDropType()) {
                    case DEFAULT:
                        // Don't change anything.
                        break;
                    case NOTHING:
                        entity.remove();
                        break;
                    case MOB:
                    case ITEM:
                        entity.remove();
                        drop.generate("Mob replacement", null, entity.getLocation());
                        break;
                    }
                } else {
                    MobType vanillaMobType = MOBS.getMobType(entity.getType());
                    if (vanillaMobType != null) {
                        vanillaMobType.configureMob(entity);
                    }
                }
            }
            break;

        default:
            break;
        }
    } // onCreatureSpawn

    // ------------------------------------------------------------------------
    /**
     * Tag mobs hurt by players with the time stamp of the damage event to
     * facilitate custom dropped XP.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        boolean isPlayerAttack = false;
        if (event.getDamager() instanceof Player) {
            isPlayerAttack = true;
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                isPlayerAttack = true;
            }
        }

        // Tag mobs hurt by players with the damage time stamp.
        if (isPlayerAttack) {
            entity.setMetadata(PLAYER_DAMAGE_TIME_KEY, new FixedMetadataValue(this, new Long(entity.getWorld().getFullTime())));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle entity death of custom mobs by replacing drops.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        // Note: Ghasts and Slimes are not Monsters... Players and ArmorStands
        // are LivingEntities. #currentyear
        String mobTypeId = (String) EntityMeta.api().get(entity, this, "mob-type");
        MobType mobType = MOBS.getMobType(mobTypeId);
        if (mobType != null) {
            DropSet drops = mobType.getDrops();
            if (drops != null) {
                StringBuilder trigger = new StringBuilder();
                // TODO: get player name from (transient) Metadata
                Player player = null;
                trigger.append("<playername>");
                trigger.append(" killed ");
                trigger.append(mobTypeId);

                boolean dropDefaultItems = drops.generateRandomDrops(trigger.toString(), player, entity.getLocation());
                if (!dropDefaultItems) {
                    event.getDrops().clear();
                }
            }

            // Drop head, armour and hand items with non-vanilla code if
            // those drop percent properties are set. This is a work around for
            // a presumed Spigot bug wherein NBT drop HandItemDropChances and
            // ArmorDropChances values are negative despite the Bukkit API
            // drop chances being sane. Bukkit API drop chances are therefore
            // set 0 in MobType when the properties are set.
            // If the entity has a MobType, it's a LivingEntity.
            LivingEntity mob = (LivingEntity) entity;
            Location loc = mob.getLocation();
            World world = loc.getWorld();

            Double helmetPercent = (Double) mobType.getDerivedProperty("helmet-drop-percent").getValue();
            if (helmetPercent != null && Util.random() < helmetPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getHelmet());
            }
            Double chestPercent = (Double) mobType.getDerivedProperty("chest-plate-drop-percent").getValue();
            if (chestPercent != null && Util.random() < chestPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getChestplate());
            }
            Double leggingsPercent = (Double) mobType.getDerivedProperty("leggings-drop-percent").getValue();
            if (leggingsPercent != null && Util.random() < leggingsPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getLeggings());
            }
            Double bootsPercent = (Double) mobType.getDerivedProperty("boots-drop-percent").getValue();
            if (bootsPercent != null && Util.random() < bootsPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getBoots());
            }
            Double mainHandPercent = (Double) mobType.getDerivedProperty("main-hand-drop-percent").getValue();
            if (mainHandPercent != null && Util.random() < mainHandPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getItemInMainHand());
            }
            Double offHandPercent = (Double) mobType.getDerivedProperty("off-hand-drop-percent").getValue();
            if (offHandPercent != null && Util.random() < offHandPercent / 100) {
                world.dropItem(loc, mob.getEquipment().getItemInOffHand());
            }

            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                if (loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    MobProperty experience = mobType.getDerivedProperty("experience");
                    if (experience.getValue() != null) {
                        event.setDroppedExp((Integer) experience.getValue());
                    }
                }
            }
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Handle block break in a zone where that block type should drop custom
     * drops.
     * 
     * Only survival mode players should trigger drops.
     * 
     * @param event the BlockBreakEvent.
     * @param block the broken block.
     */
    protected void handleBlockBreakCustomDrops(BlockBreakEvent event, Block block) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Zone zone = ZONES.getZone(loc);
        if (zone == null) {
            return;
        }

        DropSet drops = zone.getMiningDrops(block.getType());
        if (drops == null) {
            return;
        }

        StringBuilder trigger = new StringBuilder();
        trigger.append(event.getPlayer().getName());
        trigger.append(" broke ");
        trigger.append(block.getType());
        if (block.getData() != 0) {
            trigger.append(':').append(block.getData());
        }

        boolean dropDefaultItems = drops.generateRandomDrops(trigger.toString(), event.getPlayer(), loc);
        event.setDropItems(dropDefaultItems);
    }

    // ------------------------------------------------------------------------
    /**
     * In the plains biome in the nether environment, replace the configured
     * percentage of Skeletons with WitherSkeletons.
     * 
     * This code dates back to PvE Rev 19 when vanilla Minecraft separated
     * wither skeletons from regular skeltons, breaking wither spawning in
     * nether plains biomes. It will eventually be obsoleted by more general
     * BeastMaster mechanisms.
     */
    protected void replaceNetherSkeletonSpawn(CreatureSpawnEvent event) {
        // Old PvE Rev 19 code path to make Wither Skeletons spawn in nether
        // plains biomes following removal from vanilla.
        Location loc = event.getLocation();
        World world = loc.getWorld();
        if (world.getEnvironment() == Environment.NETHER &&
            loc.getBlock().getBiome() == Biome.PLAINS &&
            event.getEntityType() == EntityType.SKELETON &&
            Math.random() < CONFIG.CHANCE_WITHER_SKELETON) {
            if (CONFIG.DEBUG_REPLACE) {
                getLogger().info(String.format("Replacing skeleton at (%d, %d, %d, %s) with wither skeleton.",
                                               loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
            }
            event.getEntity().remove();
            world.spawnEntity(loc, EntityType.WITHER_SKELETON);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the world time when a player damaged the specified entity, if
     * stored as a PLAYER_DAMAGE_TIME_KEY metadata value, or null if that didn't
     * happen.
     *
     * @param entity the entity (mob).
     * @return the damage time stamp as Long, or null.
     */
    protected Long getPlayerDamageTime(Entity entity) {
        List<MetadataValue> playerDamageTime = entity.getMetadata(PLAYER_DAMAGE_TIME_KEY);
        if (playerDamageTime.size() > 0) {
            MetadataValue value = playerDamageTime.get(0);
            if (value.value() instanceof Long) {
                return (Long) value.value();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity can fit at its spawn location.
     * 
     * This check is important when replacing mobs, because the replacement may
     * be larger than the mob it replaced and may suffocate. Mobs that don't fit
     * are simply removed.
     * 
     * @return true if the specified entity can fit at its spawn location.
     */
    protected boolean canFit(Entity entity) {
        int height = (int) Math.ceil(entity.getHeight());
        int width = (int) Math.ceil(entity.getWidth());

        // For 1x1 mobs, assume that they are spawning in the block just broken.
        // Just let them spawn, even though the block is still there until the
        // end of the tick.
        if (width == 1 && height == 1) {
            return true;
        }

        Block feetBlock = entity.getLocation().getBlock();
        for (int y = height - 1; y >= 0; --y) {
            for (int x = -width / 2; x <= width / 2; ++x) {
                for (int z = -width / 2; z <= width / 2; ++z) {
                    Block block = feetBlock.getRelative(x, y, z);
                    if (block != null && block.getType() != Material.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified CommandExecutor and set it as its own TabCompleter.
     * 
     * @param executor the CommandExecutor.
     */
    protected void addCommandExecutor(ExecutorBase executor) {
        PluginCommand command = getCommand(executor.getName());
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    // ------------------------------------------------------------------------
    /**
     * Metadata name used for metadata stored on mobs to record last damage time
     * (Long) by a player.
     */
    protected static final String PLAYER_DAMAGE_TIME_KEY = "BM_PlayerDamageTime";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * When spawning a custom mob via
     * {@link #spawnMob(Location, EntityType, MobType)} we need to record the
     * MobType of the custom mob so that the entity can be configured
     * accordingly. BeastMaster.spawnMob() calls World.spawnEntity() which
     * triggers a function call to onCreatureSpawn(), which then consults this
     * field only in the event of SpawnReason.CUSTOM. This field is then cleared
     * to null to prevent BeastMaster from attempting custom configuration of
     * mobs spawned by other plugins.
     */
    protected MobType _spawningMobType;

    /**
     * If true, and if _spawningMobType is non-null, then a size check is made
     * on mobs with SpawnReason.CUSTOM and those that don't fit the available
     * space at their spawn location are removed.
     * 
     * The size check is there to gracefully handle replacement of naturally
     * spawned mobs with different size mobs. However, when spawnMob() is called
     * because of the /beast-mob spawn or /beast-mob statue commands, the size
     * check should be suppressed to avoid the confusion of nothing spawning as
     * a result.
     */
    protected boolean _spawningCheckCanFit;
} // class BeastMaster