package nu.nerd.beastmaster.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import nu.nerd.beastmaster.BeastMaster;

// ----------------------------------------------------------------------------
/**
 * Manages the zone definitions.
 */
public class ZoneManager {
    // ------------------------------------------------------------------------
    /**
     * Return the zone with the specified name.
     * 
     * @param name the zone name.
     * @return the zone.
     */
    public Zone getZone(String id) {
        return _idToZone.get(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Zone corresponding to the specified Location, or null if there
     * is none.
     * 
     * @param loc the Location.
     * @return the Zone, or null.
     */
    public Zone getZone(Location loc) {
        ArrayList<Zone> worldZones = _worldToZones.get(loc.getWorld());
        if (worldZones == null) {
            return null;
        }

        for (Zone zone : worldZones) {
            if (zone.contains(loc)) {
                return zone;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new zone.
     * 
     * @param zone the zone. The zone should not already exist within the
     *        manager.
     */
    public void addZone(Zone zone) {
        _idToZone.put(zone.getId(), zone);

        ArrayList<Zone> worldZones = _worldToZones.get(zone.getWorld());
        if (worldZones == null) {
            worldZones = new ArrayList<Zone>();
            _worldToZones.put(zone.getWorld(), worldZones);
        }

        worldZones.add(zone);
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new zone.
     * 
     * @param zone the zone.
     */
    public void removeZone(Zone zone) {
        _idToZone.remove(zone.getId());

        ArrayList<Zone> worldZones = _worldToZones.get(zone.getWorld());
        worldZones.remove(zone);
        if (worldZones.isEmpty()) {
            _worldToZones.remove(zone.getWorld());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all zones.
     * 
     * @return a collection of all zones.
     */
    public Collection<Zone> getZones() {
        return _idToZone.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a non-null list of all zones in the specified world.
     * 
     * @param world the World.
     * @return a non-null list of all zones in the specified world (may be
     *         empty).
     */
    public ArrayList<Zone> getZones(World world) {
        ArrayList<Zone> worldZones = _worldToZones.get(world);
        return (worldZones != null) ? worldZones : new ArrayList<Zone>();
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the zones from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        ConfigurationSection zones = config.getConfigurationSection("zones");
        if (zones == null) {
            zones = config.createSection("zones");
        }

        for (String id : zones.getKeys(false)) {
            ConfigurationSection section = zones.getConfigurationSection(id);
            Zone zone = new Zone(id, null);
            if (zone.load(section, logger)) {
                addZone(zone);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the zones in the plugin configuration and write that to disk.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create zones section empty to remove deleted zones.
        ConfigurationSection zones = config.createSection("zones");
        for (Zone zone : _idToZone.values()) {
            zone.save(zones, logger);
        }
        BeastMaster.CONFIG.save();
    }

    // ------------------------------------------------------------------------
    /**
     * Map from zone ID to corresponding zones.
     */
    protected HashMap<String, Zone> _idToZone = new HashMap<>();

    /**
     * Map from world to list of zones in that world.
     */
    protected HashMap<World, ArrayList<Zone>> _worldToZones = new HashMap<>();
} // class ZoneManager