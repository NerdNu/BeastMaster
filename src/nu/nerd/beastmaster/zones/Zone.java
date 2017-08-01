package nu.nerd.beastmaster.zones;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import nu.nerd.beastmaster.WeightedSelection;

// ----------------------------------------------------------------------------
/**
 * Describes the zone of operation of beasts.
 */
public class Zone {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the identifier.
     * @param world the containing world.
     */
    public Zone(String id, World world) {
        _id = id;
        _world = world;
    }

    // ------------------------------------------------------------------------
    /**
     * Load this zone from the specified configuration section, whose name is
     * the zone ID.
     * 
     * @param section the configuration section.
     * @param logger the logger.
     * @return true if the zone was loaded successfully.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _id = section.getName();

        String worldName = section.getString("world");
        _world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
        if (_world == null) {
            logger.severe("Could not load zone: " + getId() + ": invalid world " + worldName);
            return false;
        }

        _spawns.clear();
        ConfigurationSection spawnsSection = section.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            for (String mobTypeId : spawnsSection.getKeys(false)) {
                _spawns.addChoice(mobTypeId, spawnsSection.getDouble(mobTypeId));
            }
        }

        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this zone as a child of the specified parent configuration section.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        section.set("world", _world.getName());

        ConfigurationSection spawnsSection = section.createSection("spawns");
        for (Entry<String, Double> entry : getSpawnWeights().entrySet()) {
            spawnsSection.set(entry.getKey(), entry.getValue());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this zone.
     * 
     * @return the programmatic ID of this zone.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the zone contains the specified location.
     * 
     * @param loc the Location.
     * @return true if the zone contains the specified location.
     */
    public boolean contains(Location loc) {
        return loc.getWorld().equals(getWorld());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the World containing this zone.
     * 
     * @return the World containing this zone.
     */
    public World getWorld() {
        return _world;
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified mob type (by ID) as a spawn replacement in this zone.
     * 
     * @param mobTypeId the ID of the mob type (which must already be defined).
     * @param weight the relative likelihood of the spawn; only meaningful in
     *        comparison to other weights in this zone.
     */
    public void addSpawn(String mobTypeId, double weight) {
        removeSpawn(mobTypeId);
        _spawns.addChoice(mobTypeId, weight);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the specified mob type (by ID) as a spawn replacement in this
     * zone.
     * 
     * @param mobTypeId the ID of the mob type.
     * @return the removed mob type ID, if found.
     */
    public String removeSpawn(String mobTypeId) {
        return _spawns.removeChoice(mobTypeId);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a map of to spawned mob type identifier to spawn weight.
     * 
     * @return a map of to spawned mob type identifier to spawn weight.
     */
    public Map<String, Double> getSpawnWeights() {
        Map<String, Double> spawns = new TreeMap<>();
        double previousKey = 0;
        for (Entry<Double, String> entry : _spawns.entrySet()) {
            double weight = entry.getKey() - previousKey;
            previousKey = entry.getKey();
            spawns.put(entry.getValue(), weight);
        }
        return spawns;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the total of all spawn weights in the zone.
     * 
     * @return the total of all spawn weights in the zone.
     */
    public double getTotalWeight() {
        return _spawns.getTotalWeight();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the human-readable description of the zone.
     * 
     * @return the human-readable description of the zone.
     */
    public String getDescription() {
        return ChatColor.YELLOW + _id +
               ChatColor.WHITE + " in world " +
               ChatColor.YELLOW + _world.getName();
    }

    // ------------------------------------------------------------------------
    /**
     * Unique programmatic identifier.
     */
    protected String _id;

    /**
     * The world containing the zone.
     */
    protected World _world;

    /**
     * Weighted selection of string IDs of mob types that replace spawns in this
     * zone.
     */
    protected WeightedSelection<String> _spawns = new WeightedSelection<>();

} // class Zone