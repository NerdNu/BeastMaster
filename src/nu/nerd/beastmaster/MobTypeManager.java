package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Manages the collection of all known custom {@link MobType}s.
 */
public class MobTypeManager {
    // ------------------------------------------------------------------------
    /**
     * Return the {@link MobType} with the specified ID, or null if not found.
     * 
     * @return the {@link MobType} with the specified ID, or null if not found.
     */
    public MobType getMobType(String id) {
        return _types.get(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link MobTypes}.
     * 
     * @return a collection of all {@link MobTypes}.
     */
    public Collection<MobType> getMobTypes() {
        return _types.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new {@link MobType}.
     * 
     * The type should not be previously registered.
     */
    public void addMobType(MobType type) {
        _types.put(type.getId(), type);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param id the ID of the type to remove.
     */
    public void removeMobType(String id) {
        _types.remove(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param type the type to remove.
     */
    public void removeMobType(MobType type) {
        _types.remove(type.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the mob types from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _types.clear();

        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) {
            mobsSection = config.createSection("mobs");
        }

        for (String id : mobsSection.getKeys(false)) {
            ConfigurationSection section = mobsSection.getConfigurationSection(id);
            MobType mobType = new MobType(id, null);
            if (mobType.load(section, logger)) {
                addMobType(mobType);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the mob types in the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create mobs section empty to remove delete mob types.
        ConfigurationSection mobsSection = config.createSection("mobs");
        for (MobType mobType : _types.values()) {
            mobType.save(mobsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@link MobType} ID to instance.
     */
    protected HashMap<String, MobType> _types = new HashMap<>();
} // class MobTypeManager
