package nu.nerd.beastmaster.objectives;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Manages a collection of all known {@link ObjectiveType}s.
 */
public class ObjectiveTypeManager {
    // ------------------------------------------------------------------------
    /**
     * Add a new objective type.
     * 
     * @param objectiveType the objective type to add.
     */
    public void addObjectiveType(ObjectiveType objectiveType) {
        _types.put(objectiveType.getId(), objectiveType);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove and return an objective type.
     * 
     * @param objectiveType the objective type to remove.
     * @return the removed object, or null if not found.
     */
    public ObjectiveType removeObjectiveType(ObjectiveType objectiveType) {
        return removeObjectiveType(objectiveType.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove and return an objective type.
     * 
     * @param id the ID of the objective type to remove.
     * @return the removed object, or null if not found.
     */
    public ObjectiveType removeObjectiveType(String id) {
        return _types.remove(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the objective type with the specified ID.
     * 
     * @param id the ID.
     * @return the objective type, or null if not found.
     */
    public ObjectiveType getObjectiveType(String id) {
        return _types.get(id);
    }

    // --------------------------------------------------------------------------
    /**
     * Return a collection of all objective types.
     * 
     * @return a collection of all objective types.
     */
    public Collection<ObjectiveType> getObjectiveTypes() {
        return _types.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the objective types from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _types.clear();

        ConfigurationSection objectivesSection = config.getConfigurationSection("objectives");
        if (objectivesSection == null) {
            objectivesSection = config.createSection("objectives");
        }

        for (String id : objectivesSection.getKeys(false)) {
            ConfigurationSection section = objectivesSection.getConfigurationSection(id);
            ObjectiveType objectiveType = new ObjectiveType(id);
            if (objectiveType.load(section, logger)) {
                addObjectiveType(objectiveType);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the objective types in the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create mobs section empty to remove delete mob types.
        ConfigurationSection objectivesSection = config.createSection("objectives");
        for (ObjectiveType objectiveType : _types.values()) {
            objectiveType.save(objectivesSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@linl ObjectiveType} ID to instance.
     */
    protected HashMap<String, ObjectiveType> _types = new HashMap<>();

} // class ObjectiveTypeManager