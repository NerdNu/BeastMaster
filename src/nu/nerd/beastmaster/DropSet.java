package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

// ----------------------------------------------------------------------------
/**
 * Represents a set of drops.
 */
public class DropSet {
    // ------------------------------------------------------------------------
    /**
     * Remove all drops.
     */
    public void clear() {
        _drops.clear();
    }

    // ------------------------------------------------------------------------
    /**
     * Add or replace the drop with the item ID of the specified Drop.
     * 
     * @param drop the drop.
     */
    public void addDrop(Drop drop) {
        _drops.put(drop.getItemId(), drop);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the drop with the specified item ID.
     * 
     * @param itemId the item ID.
     * @return the removed drop.
     */
    public Drop removeDrop(String itemId) {
        return _drops.remove(itemId);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the drop with the same item ID as the specified drop from the
     * drops of this mob.
     * 
     * @param drop the drop.
     * @return the removed drop.
     */
    public Drop removeDrop(Drop drop) {
        return removeDrop(drop.getItemId());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the drop with the specified item ID, or null if not found.
     * 
     * @param itemId the item ID.
     * @return the drop with the specified item ID, or null if not found.
     */
    public Drop getDrop(String itemId) {
        return _drops.get(itemId);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all drops of this mob type.
     * 
     * @return a collection of all drops of this mob type.
     */
    public Collection<Drop> getAllDrops() {
        return _drops.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Process all drops and randomly drop them at the specified Location.
     * 
     * @param loc the Location.
     */
    public void drop(Location loc) {
        for (Drop drop : _drops.values()) {
            if (Math.random() < drop.getDropChance()) {
                loc.getWorld().dropItemNaturally(loc, drop.generate());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load all drops from the specified configuration section, with each key
     * being an item ID.
     * 
     * @param section the section.
     * @param logger the logger for messages.
     */
    public void load(ConfigurationSection section, Logger logger) {
        _drops.clear();
        if (section != null) {
            for (String itemId : section.getKeys(false)) {
                Drop drop = new Drop(itemId, 0, 0, 0);
                ConfigurationSection dropSection = section.getConfigurationSection(itemId);
                if (drop.load(dropSection, logger)) {
                    _drops.put(itemId, drop);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all drops to the specified section, with each item corresponding to
     * one key.
     * 
     * @param section the configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection section, Logger logger) {
        for (Drop drop : _drops.values()) {
            drop.save(section, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from item ID to drop for this mob type.
     */
    protected HashMap<String, Drop> _drops = new HashMap<>();

} // class DropSet