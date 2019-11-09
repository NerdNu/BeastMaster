package nu.nerd.beastmaster;

import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;

import net.sothatsit.blockstore.BlockStoreApi;
import nu.nerd.beastmaster.commands.BeastItemExecutor;
import nu.nerd.beastmaster.commands.BeastLootExecutor;
import nu.nerd.beastmaster.commands.BeastMasterExecutor;
import nu.nerd.beastmaster.commands.BeastMobExecutor;
import nu.nerd.beastmaster.commands.BeastObjectiveExecutor;
import nu.nerd.beastmaster.commands.BeastPotionExecutor;
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
     * Manages all potion sets.
     */
    public static final PotionManager POTIONS = new PotionManager();

    /**
     * Keeps track of all disguised mobs.
     */
    public static final DisguiseManager DISGUISES = new DisguiseManager();

    // ------------------------------------------------------------------------
    /**
     * Log a debug message.
     * 
     * @param message the message.
     */
    public void debug(String message) {
        getLogger().info("[DEBUG] " + message);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        saveDefaultConfig();
        CONFIG.reload(false);

        addCommandExecutor(new BeastMasterExecutor());
        addCommandExecutor(new BeastZoneExecutor());
        addCommandExecutor(new BeastItemExecutor());
        addCommandExecutor(new BeastLootExecutor());
        addCommandExecutor(new BeastMobExecutor());
        addCommandExecutor(new BeastObjectiveExecutor());
        addCommandExecutor(new BeastPotionExecutor());

        getServer().getPluginManager().registerEvents(this, this);

        // Every tick, do particle effects for objectives.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                OBJECTIVES.tickAll();
            }
        }, 1, 1);

        OBJECTIVES.extractSchematics();

        // Since we can't rely on ChunkLoadEvent or WorldLoadEvent to tell us
        // when chunks containing disguised mobs load at startup, let's
        // process all loaded chunks here.
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                DISGUISES.loadDisguises(chunk);
            }
        }
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
     * Return the MobType of the specified entity.
     * 
     * @param entity the entity.
     * @return the MobType, or null if the entity is not living, or has no
     *         custom MobType. Note that generally vanilla mobs do have one even
     *         if it has not custom properties.
     */
    public static MobType getMobType(Entity entity) {
        if (entity instanceof LivingEntity) {
            String mobTypeId = (String) EntityMeta.api().get(entity, BeastMaster.PLUGIN, "mob-type");
            return MOBS.getMobType(mobTypeId);
        }
        return null;
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
        getLogger().info("onWorldLoad: " + event.getWorld().getName());
        World world = event.getWorld();
        if (ZONES.getZone(world.getName()) == null) {
            ZONES.addZone(new Zone(world.getName(), world));
        }

        // ChunkLoadEvent is not raised for pre-loaded spawn chunks.
        for (Chunk chunk : world.getLoadedChunks()) {
            Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.loadDisguises(chunk), 1);
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
                debug("BlockStoreApi.isPlaced() took: " + String.format("%3.2f", elapsedMs));
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

        case NATURAL:
        case CHUNK_GEN:
        case REINFORCEMENTS:
        case INFECTION:
        case VILLAGE_INVASION:
        case VILLAGE_DEFENSE:
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

        case EGG:
        case SPAWNER_EGG:
        case BUILD_SNOWMAN:
        case BUILD_IRONGOLEM:
        case BUILD_WITHER:
            MobType vanillaMobType = MOBS.getMobType(entity.getType());
            if (vanillaMobType != null) {
                vanillaMobType.configureMob(entity);
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
     * 
     * Also, apply attack-potions {@link PotionSet} to the victim, where
     * configured on the attacker.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagedEntity = event.getEntity();
        LivingEntity attackingMob = null;

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_NAME, attacker.getName());
            EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_TIME, damagedEntity.getWorld().getFullTime());
        } else if (event.getDamager() instanceof LivingEntity) {
            attackingMob = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player attacker = (Player) projectile.getShooter();
                EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_NAME, attacker.getName());
                EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_TIME, damagedEntity.getWorld().getFullTime());
            } else if (projectile.getShooter() instanceof LivingEntity) {
                attackingMob = (LivingEntity) projectile.getShooter();
            }
        }

        // Apply attackingMob's attack-potions, if set.
        if (attackingMob != null && damagedEntity instanceof LivingEntity) {
            MobType mobType = getMobType(attackingMob);
            if (mobType != null) {
                String potionSetId = (String) mobType.getDerivedProperty("attack-potions").getValue();
                PotionSet potionSet = POTIONS.getPotionSet(potionSetId);
                if (potionSet != null) {
                    potionSet.apply((LivingEntity) damagedEntity);
                }
            }
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
        MobType mobType = getMobType(entity);
        if (mobType != null) {
            Location loc = entity.getLocation();

            // If the mob has been damaged by a player recently, work out that
            // player's name.
            String victoriousPlayerName = null;
            Long damageTime = (Long) EntityMeta.api().get(entity, this, DAMAGED_BY_PLAYER_TIME);
            if (damageTime != null) {
                if (loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    MobProperty experience = mobType.getDerivedProperty("experience");
                    if (experience.getValue() != null) {
                        event.setDroppedExp((Integer) experience.getValue());
                    }

                    victoriousPlayerName = (String) EntityMeta.api().get(entity, this, DAMAGED_BY_PLAYER_NAME);
                }
            }

            DropSet drops = mobType.getDrops();
            if (drops != null) {
                StringBuilder trigger = new StringBuilder();

                Player victoriousPlayer = (victoriousPlayerName != null) ? Bukkit.getPlayerExact(victoriousPlayerName) : null;
                trigger.append((victoriousPlayer != null) ? victoriousPlayer.getName() : "<environment>");
                trigger.append(" killed ");
                trigger.append(mobType.getId());

                boolean dropDefaultItems = drops.generateRandomDrops(trigger.toString(), victoriousPlayer, entity.getLocation());
                if (!dropDefaultItems) {
                    event.getDrops().clear();
                }
            }

            if (CONFIG.DEBUG_EQUIPMENT_DROPS) {
                // If the entity has a MobType, it's a LivingEntity.
                LivingEntity mob = (LivingEntity) entity;
                EntityEquipment equipment = mob.getEquipment();
                debug(String.format("%s equipment drop %% (B,L,C,H), (M,O): (%.3f,%.3f,%.3f,%.3f), (%.3f,%.3f)",
                                    mobType.getId(),
                                    100 * equipment.getBootsDropChance(), 100 * equipment.getLeggingsDropChance(),
                                    100 * equipment.getChestplateDropChance(), 100 * equipment.getHelmetDropChance(),
                                    100 * equipment.getItemInMainHandDropChance(), 100 * equipment.getItemInOffHandDropChance()));
                debug(mobType.getId() + " event drops: " + event.getDrops().stream().map(Util::getItemDescription).collect(Collectors.joining(", ")));
            }
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * When the player joins, send them all pertinent disguises in the world.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerJoin(PlayerJoinEvent event) {
        if (BeastMaster.CONFIG.DEBUG_DISGUISES) {
            debug("onPlayerJoin()");
        }
        DISGUISES.sendAllDisguises(event.getPlayer().getWorld(), event.getPlayer());
    }

    // ------------------------------------------------------------------------
    /**
     * When the player respawns, refresh disguises.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.sendAllDisguises(event.getPlayer().getWorld(), event.getPlayer()), 1);
    }

    // ------------------------------------------------------------------------
    /**
     * When the player changes world, send disguises for the new world.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerTeleport(PlayerTeleportEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo().getWorld();
        if (!fromWorld.equals(toWorld)) {
            Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.sendAllDisguises(toWorld, event.getPlayer()), 1);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When loading a chunk, apply disguises if not null/empty.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onChunkLoad(ChunkLoadEvent event) {
        DISGUISES.loadDisguises(event.getChunk());
    }

    // ------------------------------------------------------------------------
    /**
     * When unloading a chunk, clear disguises of unloaded mobs.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                MobType mobType = getMobType(entity);
                if (mobType != null) {
                    String encodedDisguise = (String) mobType.getDerivedProperty("disguise").getValue();
                    if (encodedDisguise != null && !encodedDisguise.isEmpty()) {
                        DISGUISES.destroyDisguise((LivingEntity) entity, event.getWorld());
                    }
                }
            }
        }
    }

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
                debug(String.format("Replacing skeleton at (%d, %d, %d, %s) with wither skeleton.",
                                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
            }
            event.getEntity().remove();
            world.spawnEntity(loc, EntityType.WITHER_SKELETON);
        }
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
     * Persistent metadata key used to record name of player that damaged a mob.
     */
    protected static final String DAMAGED_BY_PLAYER_NAME = "damage-player";

    /**
     * Persistent metadata key used to record the full world time when a mob was
     * damaged by a player.
     */
    protected static final String DAMAGED_BY_PLAYER_TIME = "damage-time";

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