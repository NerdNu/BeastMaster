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
     */
    public LivingEntity spawnMob(Location loc, MobType mobType) {
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
            livingEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);
            _spawningMobType = null;
        }
        return livingEntity;
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
                if (canFit(entity)) {
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
            break;

        default:
            break;
        }
    } // onCreatureSpawn

    // ------------------------------------------------------------------------
    /**
     * A late attempt to customise custom-spawned mobs from other plugins
     * without spawning an entire new mob.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        boolean isPlayerAttack = false;
        if (event.getDamager() instanceof Player) {
            isPlayerAttack = true;
            Player player = (Player) event.getDamager();
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

            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                Location loc = entity.getLocation();
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
     * MobType of the currently spawning mob, if spawned as a custom mob via
     * {@link #spawnMob(Location, EntityType, MobType)}.
     */
    protected MobType _spawningMobType;
} // class BeastMaster