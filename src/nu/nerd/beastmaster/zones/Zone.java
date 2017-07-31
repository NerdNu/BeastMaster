package nu.nerd.beastmaster.zones;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

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
} // class Zone