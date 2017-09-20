package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

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
        return _idToType.get(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link MobType} of the specified creature, or null if not
     * found.
     * 
     * @return the {@link MobType} with the specified ID, or null if not found.
     */
    public MobType getMobType(LivingEntity entity) {
        return _entityTypeToType.get(entity.getType());
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link MobTypes}.
     * 
     * @return a collection of all {@link MobTypes}.
     */
    public Collection<MobType> getMobTypes() {
        return _idToType.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new {@link MobType}.
     * 
     * The type should not be previously registered.
     */
    public void addMobType(MobType type) {
        _idToType.put(type.getId(), type);
        _entityTypeToType.put(type.getEntityType(), type);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param id the ID of the type to remove.
     */
    public void removeMobType(String id) {
        MobType mobType = _idToType.remove(id);
        if (mobType != null) {
            _entityTypeToType.remove(mobType.getEntityType());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param type the type to remove.
     */
    public void removeMobType(MobType type) {
        removeMobType(type.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the mob types from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _idToType.clear();
        _entityTypeToType.clear();

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
        // Create mobs section empty to remove deleted mob types.
        ConfigurationSection mobsSection = config.createSection("mobs");
        for (MobType mobType : _idToType.values()) {
            mobType.save(mobsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@link MobType} ID to instance.
     */
    protected HashMap<String, MobType> _idToType = new HashMap<>();

    /**
     * Map from EntityType to instance.
     * 
     * This assumes that there is a 1:1 correspondence between EntityType and
     * custom mob type which is WRONG WRONG WRONG (!) but the best we can do at
     * short notice without a persistent metadata API (it's 2017! grumble...).
     */
    protected HashMap<EntityType, MobType> _entityTypeToType = new HashMap<>();

} // class MobTypeManager
