package nu.nerd.beastmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import nu.nerd.beastmaster.commands.BeastItemExecutor;
import nu.nerd.beastmaster.commands.BeastMasterExecutor;
import nu.nerd.beastmaster.commands.BeastMobExecutor;
import nu.nerd.beastmaster.commands.BeastObjectiveExecutor;
import nu.nerd.beastmaster.commands.BeastZoneExecutor;
import nu.nerd.beastmaster.commands.ExecutorBase;
import nu.nerd.beastmaster.objectives.ObjectiveManager;
import nu.nerd.beastmaster.objectives.ObjectiveTypeManager;
import nu.nerd.beastmaster.zones.Zone;
import nu.nerd.beastmaster.zones.ZoneManager;

// ----------------------------------------------------------------------------
/**
 * Plugin, command handling and event handler class.
 */
public class BeastMaster extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * Configuration wrapper instance.
     */
    public static Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static BeastMaster PLUGIN;

    /**
     * Zone manager as a singleton.
     */
    public static ZoneManager ZONES = new ZoneManager();

    /**
     * Mob type manager as a singleton.
     */
    public static MobTypeManager MOBS = new MobTypeManager();

    /**
     * Manages all objectives.
     */
    public static ObjectiveManager OBJECTIVES = new ObjectiveManager();

    /**
     * Manages all objective type.
     */
    public static ObjectiveTypeManager OBJECTIVE_TYPES = new ObjectiveTypeManager();

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
     * In the plains biome in the nether environment, replace the configured
     * percentage of Skeletons with WitherSkeletons.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
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

        LivingEntity entity = event.getEntity();
        Zone zone = ZONES.getZone(loc);
        if (zone == null || !(entity instanceof Monster)) {
            return;
        }

        // TODO: handling of spawn reasons should be customisable per zone.
        SpawnReason spawnReason = event.getSpawnReason();

        // Can't handle SpawnReason.CUSTOM by spawning a new mob because
        // it will create an infinite recursion.
        if (spawnReason == SpawnReason.DEFAULT ||
            spawnReason == SpawnReason.NATURAL ||
            spawnReason == SpawnReason.REINFORCEMENTS ||
            spawnReason == SpawnReason.SPAWNER ||
            spawnReason == SpawnReason.INFECTION ||
            spawnReason == SpawnReason.NETHER_PORTAL ||
            spawnReason == SpawnReason.CHUNK_GEN ||
            spawnReason == SpawnReason.VILLAGE_INVASION) {

            String mobTypeId = zone.randomSpawnMobType();
            if (mobTypeId != null) {
                // Wholesale replacement of all spawns in the zone at this
                // stage.
                // TODO: more nuanced spawn replacement.
                event.setCancelled(true);
                entity.remove();

                MobType mobType = MOBS.getMobType(mobTypeId);
                mobType.spawn(loc);
            }
        }
    } // onCreatureSpawn

    // ------------------------------------------------------------------------
    /**
     * A late attempt to customise custom-spawned mobs from other plugins
     * without spawning an entire new mob.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        configureMob(event.getEntity());
    }

    // ------------------------------------------------------------------------
    /**
     * A late attempt to customise custom-spawned mobs from other plugins
     * without spawning an entire new mob.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        configureMob(event.getEntity());
    }

    // ------------------------------------------------------------------------
    /**
     * Handle entity death of hostile mobs in zones by replacing drops.
     * 
     * TODO: We really need persistent metadata to identify a custom mob type.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        // Note: Ghasts and Slimes are not Monsters... Players and ArmorStands
        // are LivingEntities. #currentyear
        Monster monster = (Monster) entity;

        Location loc = monster.getLocation();
        Zone zone = ZONES.getZone(loc);
        if (zone == null) {
            return;
        }

        // We assume that any mob in a defined zone is an instance of a
        // custom mob, uniquely identifiable by its EntityType.
        // To do better than this, we need persistent metadata. *sigh*
        MobType mobType = MOBS.getMobType(monster);
        if (mobType != null) {
            event.getDrops().clear();
            mobType.getDropSet().drop(loc);
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Common code to configure a mob based on zone.
     */
    protected void configureMob(Entity mob) {
        if (!(mob instanceof Monster)) {
            return;
        }
        Monster monster = (Monster) mob;
        Location loc = monster.getLocation();
        Zone zone = ZONES.getZone(loc);
        if (zone == null) {
            return;
        }

        if (monster.getMetadata(MOB_META_KEY).isEmpty()) {
            // Already customised.
            return;
        }

        MobType mobType = MOBS.getMobType(monster);
        if (mobType != null) {
            mobType.configureMob(monster);
        }
    } // configureMob

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

} // class BeastMaster