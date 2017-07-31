package nu.nerd.beastmaster;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    // ------------------------------------------------------------------------
    /**
     * If true, log the configuration on reload.
     */
    public boolean DEBUG_CONFIG;

    /**
     * If true, log replacement of skeletons by wither skeletons.
     */
    public boolean DEBUG_REPLACE;

    /**
     * Probability, in the range [0.0,1.0] that a plains biome skeleton spawn in
     * the nether environment will be replaced by a wither skeleton.
     */
    public double CHANCE_WITHER_SKELETON;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        BeastMaster.PLUGIN.reloadConfig();
        FileConfiguration config = BeastMaster.PLUGIN.getConfig();
        DEBUG_CONFIG = config.getBoolean("debug.config");
        DEBUG_REPLACE = config.getBoolean("debug.replace");
        CHANCE_WITHER_SKELETON = config.getDouble("chance.wither-skeleton");

        Logger logger = BeastMaster.PLUGIN.getLogger();

        BeastMaster.ZONES.load(config, logger);

        if (DEBUG_CONFIG) {
            logger.info("Configuration:");
            logger.info("DEBUG_REPLACE: " + DEBUG_REPLACE);
            logger.info("CHANCE_WITHER_SKELETON: " + CHANCE_WITHER_SKELETON);
            logger.info("ZONES: " + BeastMaster.ZONES.getZones().stream()
            .map(z -> z.getDescription()).collect(Collectors.joining(", ")));
        }
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Save updated configuration.
     */
    public void save() {
        BeastMaster.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
} // class Configuration