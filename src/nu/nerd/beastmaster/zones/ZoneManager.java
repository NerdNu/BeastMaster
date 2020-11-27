package nu.nerd.beastmaster.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Manages the zone definitions.
 */
public class ZoneManager {
    // ------------------------------------------------------------------------
    /**
     * Return the zone with the specified name.
     *
     * @param name the case insensitive zone name.
     * @return the zone.
     */
    public Zone getZone(String id) {
        return _idToZone.get(id.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Root Zone of the specified World.
     *
     * The Zone will be created if it does not already exist.
     *
     * @return the Root Zone of the specified World.
     */
    public Zone getRootZone(World world) {
        Zone zone = getZone(world.getName());
        if (zone == null) {
            zone = new Zone(world);
        }
        return zone;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a list of all root Zones, sorted by case-insensitive ID.
     *
     * @return a list of all root Zones, sorted by case-insensitive ID.
     */
    public List<Zone> getRootZones() {
        return _idToZone.values().stream()
            .filter(Zone::isRoot)
            .sorted(Comparator.comparing(Zone::getId, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Zone corresponding to the specified Location.
     *
     * At any location, there will be at least the Root Zone of the world. That
     * is, the result will never be null.
     *
     * However, this method will return the most specific child Zone whose Zone
     * Specification is true at the specified Location.
     *
     * @param loc the Location.
     * @return the Zone, never null.
     */
    public Zone getZone(Location loc) {
        Zone root = getRootZone(loc.getWorld());
        for (int i = 0; i < root.children().size(); ++i) {
            Zone child = getChildZone(root.children().get(i), loc);
            if (child != null) {
                return child;
            }
        }
        return root;
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new zone.
     *
     * @param zone the zone. The zone should not already exist within the
     *             manager.
     */
    public void addZone(Zone zone) {
        // Guard against overwriting a pre-existing zone, e.g. when adding
        // a Root Zone in the onWorldLoad() event.
        if (getZone(zone.getId()) == null) {
            _idToZone.put(zone.getId().toLowerCase(), zone);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a zone.
     *
     * @param removed the removed zone.
     */
    public void removeZone(Zone removed) {
        _idToZone.remove(removed.getId().toLowerCase());

        // Remove reference to removed zone in parent's children list.
        Zone parent = removed.getParent();
        if (parent != null) {
            parent.children().remove(removed);
        }

        // Recursively remove children, avoiding concurrent modification.
        ArrayList<Zone> children = new ArrayList<>(removed.children());
        for (Zone child : children) {
            removeZone(child);
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
     * Load all the zones from the plugin configuration.
     *
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _idToZone.clear();

        ConfigurationSection zones = config.getConfigurationSection("zones");
        if (zones == null) {
            zones = config.createSection("zones");
        }

        for (String id : zones.getKeys(false)) {
            ConfigurationSection zoneSection = zones.getConfigurationSection(id);
            Zone zone = new Zone();
            if (zone.loadProperties(zoneSection, logger)) {
                addZone(zone);
            }
        }

        for (String id : zones.getKeys(false)) {
            ConfigurationSection zoneSection = zones.getConfigurationSection(id);
            Zone zone = getZone(id);
            zone.loadHierarchy(zoneSection, logger);
        }

        // Add in default zones for any worlds not mentioned in the config.
        for (World world : Bukkit.getWorlds()) {
            if (getZone(world.getName()) == null) {
                addZone(new Zone(world));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the zones in the plugin configuration.
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
    }

    // ------------------------------------------------------------------------
    /**
     * Return the deepest descendant of a Zone whose Zone Specification
     * evaluates to true at a Location, or null if three is no such child.
     *
     * Traversal is depth-first
     *
     * @param zone the parent Zone.
     * @loc the Location where the Zone Specification is evaluated.
     * @return the first
     *
     */
    protected static Zone getChildZone(Zone zone, Location loc) {
        if (zone.contains(loc)) {
            for (int i = 0; i < zone.children().size(); ++i) {
                Zone child = getChildZone(zone.children().get(i), loc);
                if (child != null) {
                    return child;
                }
            }
            return zone;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from lower cased zone ID to corresponding zones.
     */
    protected HashMap<String, Zone> _idToZone = new HashMap<>();

} // class ZoneManager