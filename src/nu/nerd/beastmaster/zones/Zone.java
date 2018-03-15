package nu.nerd.beastmaster.zones;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;

// ----------------------------------------------------------------------------
/**
 * A type of {@link Condition} that is predicated on only the Location of an
 * event in space.
 */
public class Zone extends Condition {
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
     * Return the set of EntityTypes replaced in this zone.
     * 
     * @return the set of EntityTypes replaced in this zone.
     */
    public Set<EntityType> getAllReplacedEntityTypes() {
        return _mobReplacementDropSetIDs.keySet();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of the DropSet of replacements for newly spawned mobs of
     * the specified EntityType in this zone.
     * 
     * @param entityType the EntityType of a newly spawned mob.
     * @return the ID of the DropSet of replacements for newly spawned mobs of
     *         the specified EntityType in this zone.
     */
    public String getMobReplacementDropSetId(EntityType entityType) {
        return _mobReplacementDropSetIDs.get(entityType);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the DropSet of replacements for newly spawned mobs of the
     * specified EntityType in this zone.
     * 
     * @param entityType the EntityType of a newly spawned mob.
     * @return the DropSet of replacements for newly spawned mobs of the
     *         specified EntityType in this zone.
     */
    public DropSet getMobReplacementDropSet(EntityType entityType) {
        return BeastMaster.LOOTS.getDropSet(getMobReplacementDropSetId(entityType));
    }

    // ------------------------------------------------------------------------
    /**
     * Set the ID of the DropSet that defines replacement of newly spawned mobs
     * of the specified EntityType in this zone.
     * 
     * @param entityType the EntityType of a newly spawned mob.
     * @param dropSetId the ID of the DropSet, or null to disable replacement.
     */
    public void setMobReplacementDropSetId(EntityType entityType, String dropSetId) {
        if (dropSetId == null) {
            _mobReplacementDropSetIDs.remove(entityType);
        } else {
            _mobReplacementDropSetIDs.put(entityType, dropSetId);
        }
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
    @Override
    public boolean load(ConfigurationSection section, Logger logger) {
        _id = section.getName();

        if (!super.load(section, logger)) {
            return false;
        }

        String worldName = section.getString("world");
        _world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
        if (_world == null) {
            logger.severe("Could not load zone: " + getId() + ": invalid world " + worldName);
            return false;
        }
        _centreX = section.getInt("centre-x");
        _centreZ = section.getInt("centre-z");
        _radius = section.getInt("radius");

        ConfigurationSection replacements = section.getConfigurationSection("replacements");
        for (String entityTypeName : replacements.getKeys(false)) {
            try {
                setMobReplacementDropSetId(EntityType.valueOf(entityTypeName), replacements.getString(entityTypeName));
            } catch (IllegalArgumentException ex) {
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
    @Override
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());

        super.save(section, logger);
        section.set("world", _world.getName());
        section.set("centre-x", _centreX);
        section.set("centre-z", _centreZ);
        section.set("radius", _radius);

        ConfigurationSection replacements = section.createSection("replacements");
        for (EntityType entityType : _mobReplacementDropSetIDs.keySet()) {
            replacements.set(entityType.toString(), getMobReplacementDropSetId(entityType));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this zone.
     * 
     * @return the programmatic ID of this zone.
     */
    @Override
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the square bounds of the zone as a centre position and a radius that
     * is half the side length of the square.
     * 
     * @param centreX the centre X coordinate.
     * @param centreZ the centre Z coordinate.
     * @param radius the radius.
     */
    public void setSquareBounds(int centreX, int centreZ, int radius) {
        _centreX = centreX;
        _centreZ = centreZ;
        _radius = radius;
    }

    // --------------------------------------------------------------------------
    /**
     * @see java.util.function.Predicate#test(java.lang.Object)
     */
    @Override
    public boolean test(Location loc) {
        return contains(loc);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the zone contains the specified location.
     * 
     * @param loc the Location.
     * @return true if the zone contains the specified location.
     */
    public boolean contains(Location loc) {
        return loc.getWorld().equals(getWorld()) &&
               Math.abs(loc.getX() - _centreX) < _radius &&
               Math.abs(loc.getZ() - _centreZ) < _radius;
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
     * Return the centre X coordinate.
     * 
     * @return the centre X coordinate.
     */
    public int getCentreX() {
        return _centreX;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the centre Z coordinate.
     * 
     * @return the centre Z coordinate.
     */
    public int getCentreZ() {
        return _centreZ;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the zone radius (half the square side length) in blocks.
     * 
     * @return the zone radius (half the square side length) in blocks.
     */
    public int getRadius() {
        return _radius;
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
               ChatColor.YELLOW + _world.getName() +
               ChatColor.WHITE + " square, radius " +
               ChatColor.YELLOW + _radius +
               ChatColor.WHITE + " centred on " +
               ChatColor.YELLOW + "(" + _centreX + "," + _centreZ + ")";
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
     * Centre X coordinate.
     */
    protected int _centreX;

    /**
     * Centre Z coordinate.
     */
    protected int _centreZ;

    /**
     * Radius of the zone (half the square side length) in blocks.
     */
    protected int _radius;

    /**
     * Map from EntityType to ID of DropSet to replace it with on spawn.
     */
    protected HashMap<EntityType, String> _mobReplacementDropSetIDs = new HashMap<>();
} // class Zone