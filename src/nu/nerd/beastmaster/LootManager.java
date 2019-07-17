package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Manages a collection of all known loot tables.
 * 
 * Loot table IDs are compared case-insensitively.
 */
public class LootManager {
    // ------------------------------------------------------------------------
    /**
     * Return the {@link DropSet} with the specified ID, or null if not found.
     * 
     * @return the {@link DropSet} with the specified ID, or null if not found.
     */
    public DropSet getDropSet(String id) {
        return id != null ? _idToDrops.get(id.toLowerCase()) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link DropSets}.
     * 
     * @return a collection of all {@link DropSets}.
     */
    public Collection<DropSet> getDropSets() {
        return _idToDrops.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new {@link DropSet}.
     * 
     * The drops should not be previously registered.
     * 
     * @param drops the loot table.
     */
    public void addDropSet(DropSet drops) {
        _idToDrops.put(drops.getId().toLowerCase(), drops);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link DropSet}.
     * 
     * @param id the ID of the loot table to remove.
     */
    public void removeDropSet(String id) {
        _idToDrops.remove(id.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link DropSet}.
     * 
     * @param drops the loot table to remove.
     */
    public void removeDropSet(DropSet drops) {
        removeDropSet(drops.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the loot tables from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _idToDrops.clear();

        ConfigurationSection lootsSection = config.getConfigurationSection("loots");
        if (lootsSection == null) {
            lootsSection = config.createSection("loots");
        }

        for (String id : lootsSection.getKeys(false)) {
            ConfigurationSection section = lootsSection.getConfigurationSection(id);
            DropSet drops = new DropSet(id);
            drops.load(section, logger);
            addDropSet(drops);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the loot tables in the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create loots section empty to remove deleted loot tables.
        ConfigurationSection lootsSection = config.createSection("loots");
        for (DropSet drops : _idToDrops.values()) {
            drops.save(lootsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@link DropSet} lower-case ID to instance.
     */
    protected HashMap<String, DropSet> _idToDrops = new HashMap<>();

} // class LootManager