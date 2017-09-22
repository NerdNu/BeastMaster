package nu.nerd.beastmaster.zones;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;

// ----------------------------------------------------------------------------
/**
 * A Predicate<> that is evaluated to determine whether BeastMaster should take
 * an action in response to an event (e.g. a mob dying, a block being mined).
 * 
 * The inherited <i> test(Location)</i> method receives the Location of the
 * event, but is free to ignore it.
 * 
 * Subclasses of Condition may test the Location against some geometrically
 * expressed zone, or test conditions at or near the Location (such as the light
 * level or biome) or can test global state like the time of day or the phase of
 * the moon.
 * 
 * TODO: HierarchyNode containing parent and child hierarchy methods.
 * 
 * TODO: move get/setMiningDrops to HierarchyNode.
 */
public abstract class Condition implements Predicate<Location> {
    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this Condition.
     * 
     * @return the programmatic ID of this Condition.
     */
    public abstract String getId();

    // ------------------------------------------------------------------------
    /**
     * Set the {@link DropSet} that will be consulted to determine what to drop
     * when a block is mined when this Condition holds.
     * 
     * @param material the type of the mined block.
     * @param dropSetId the ID of the set of drops to select from. If null,
     *        remove the entry for the specified Material.
     */
    public void setMiningDropsId(Material material, String dropSetId) {
        if (dropSetId == null) {
            _miningDropsIds.remove(material);
        } else {
            _miningDropsIds.put(material, dropSetId);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of the {@link DropSet} controlling drops when the specified
     * Material is mined.
     * 
     * @param material the type of the mined block.
     * @return the ID of the {@link DropSet} controlling drops when the
     *         specified Material is mined, or null if this Condition does not
     *         override the drops.
     */
    public String getMiningDropsId(Material material) {
        return _miningDropsIds.get(material);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link DropSet} controlling drops when the specified Material
     * is mined.
     * 
     * @param material the type of the mined block.
     * @return the {@link DropSet} controlling drops when the specified Material
     *         is mined, or null if this Condition does not override the drops.
     */
    public DropSet getMiningDrops(Material material) {
        String id = getMiningDropsId(material);
        return (id != null) ? BeastMaster.LOOTS.getDropSet(id) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the set of all entries in the map of Materials to corresponding
     * mining loot tables.
     * 
     * @return the set of all entries in the map of Materials to corresponding
     *         mining loot tables.
     */
    public Set<Entry<Material, String>> getAllMiningDrops() {
        return _miningDropsIds.entrySet();
    }

    // ------------------------------------------------------------------------
    /**
     * Load this Condition from the specified section.
     * 
     * The mining-drops child section contains a key for each Material that maps
     * to the ID of the corresponding {@link DropSet}.
     * 
     * @param section the section.
     * @param logger the logger for messages.
     * @return true if the condition was loaded successfully.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _miningDropsIds.clear();
        ConfigurationSection miningDropsSection = section.getConfigurationSection("mining-drops");
        if (miningDropsSection != null) {
            for (String materialName : miningDropsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    String dropSetId = miningDropsSection.getString(materialName);
                    setMiningDropsId(material, dropSetId);
                } catch (IllegalArgumentException ex) {
                    logger.severe(getId() + " defined mining drops that could not be loaded for unknown material " +
                                  materialName);
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this condition to the specified configuration section.
     * 
     * @param conditionSection the configuration section corresponding to this
     *        Condition, created by the caller.
     * @param logger the logger.
     */
    public void save(ConfigurationSection conditionSection, Logger logger) {
        ConfigurationSection miningDropsSection = conditionSection.createSection("mining-drops");
        for (Entry<Material, String> entry : _miningDropsIds.entrySet()) {
            miningDropsSection.set(entry.getKey().toString(), entry.getValue());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from mined block type to ID of corresponding {@link DropSet}.
     */
    protected HashMap<Material, String> _miningDropsIds = new HashMap<>();
} // class Condition