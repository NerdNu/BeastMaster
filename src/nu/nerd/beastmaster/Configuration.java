package nu.nerd.beastmaster;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

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

    /**
     * Custom items for drops. Each ItemStack will have size 1.
     */
    public HashMap<String, ItemStack> ITEMS = new HashMap<>();

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

        ITEMS.clear();
        ConfigurationSection items = config.getConfigurationSection("items");
        for (String itemId : items.getKeys(false)) {
            ITEMS.put(itemId, items.getItemStack(itemId));
        }

        Logger logger = BeastMaster.PLUGIN.getLogger();
        BeastMaster.ZONES.load(config, logger);
        BeastMaster.MOBS.load(config, logger);

        if (DEBUG_CONFIG) {
            logger.info("Configuration:");
            logger.info("DEBUG_REPLACE: " + DEBUG_REPLACE);
            logger.info("CHANCE_WITHER_SKELETON: " + CHANCE_WITHER_SKELETON);
            logger.info("ZONES: " + BeastMaster.ZONES.getZones().stream()
            .map(z -> z.getDescription()).collect(Collectors.joining(", ")));
            logger.info("ITEMS: ");
            for (Entry<String, ItemStack> entry : ITEMS.entrySet()) {
                logger.info(entry.getKey() + ": " + Util.getItemDescription(entry.getValue()));
            }
        }
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Save updated configuration.
     */
    public void save() {
        FileConfiguration config = BeastMaster.PLUGIN.getConfig();
        Logger logger = BeastMaster.PLUGIN.getLogger();

        ConfigurationSection items = config.createSection("items");
        for (Entry<String, ItemStack> entry : ITEMS.entrySet()) {
            items.set(entry.getKey(), entry.getValue());
        }
        BeastMaster.ZONES.save(config, logger);
        BeastMaster.MOBS.save(config, logger);
        BeastMaster.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
} // class Configuration