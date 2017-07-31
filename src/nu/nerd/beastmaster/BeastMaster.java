package nu.nerd.beastmaster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import nu.nerd.beastmaster.commands.BeastMasterExecutor;
import nu.nerd.beastmaster.commands.BeastZoneExecutor;
import nu.nerd.beastmaster.commands.ExecutorBase;
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

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        saveDefaultConfig();
        CONFIG.reload();

        addCommandExecutor(new BeastMasterExecutor());
        addCommandExecutor(new BeastZoneExecutor());

        getServer().getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    // ------------------------------------------------------------------------
    /**
     * In the plains biome in the nether environment, replace the configured
     * percentage of Skeletons with WitherSkeletons.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
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
     * Add the specified CommandExecutor and set it as its own TabCompleter.
     * 
     * @param executor the CommandExecutor.
     */
    protected void addCommandExecutor(ExecutorBase executor) {
        PluginCommand command = getCommand(executor.getName());
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
} // class BeastMaster