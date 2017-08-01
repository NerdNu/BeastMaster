package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

// ----------------------------------------------------------------------------
/**
 * Stores settings for a type of mob.
 */
public class MobType {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the programmatic ID of this mob type.
     * @param entityType the EntityType of the underlying vanilla mob.
     */
    public MobType(String id, EntityType entityType) {
        _id = id;
        _entityType = entityType;
    }

    // ------------------------------------------------------------------------
    /**
     * Load this mob type from the specified section.
     * 
     * @param section the configuration file section.
     * @return true if successful.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        try {
            _entityType = EntityType.valueOf(section.getString("entity-type"));
        } catch (IllegalArgumentException ex) {
            logger.severe("Could not load entity type for mob " + getId());
            return false;
        }
        _babyFraction = (Double) section.get("baby-fraction");
        _speed = (Double) section.get("speed");
        _health = (Double) section.get("health");

        _drops.clear();
        ConfigurationSection drops = section.getConfigurationSection("drops");
        if (drops != null) {
            for (String itemId : drops.getKeys(false)) {
                Drop drop = new Drop(itemId, 0, 0, 0);
                ConfigurationSection dropSection = drops.getConfigurationSection(itemId);
                if (drop.load(dropSection, logger)) {
                    _drops.put(itemId, drop);
                }
            }
        }

        // TODO: implement potions.
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this mob type as a child of the specified parent configuration
     * section.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        section.set("entity-type", _entityType.toString());
        if (_babyFraction != null) {
            section.set("baby-fraction", _babyFraction);
        }
        if (_speed != null) {
            section.set("speed", _speed);
        }
        if (_health != null) {
            section.set("health", _health);
        }

        ConfigurationSection dropsSection = section.createSection("drops");
        for (Drop drop : _drops.values()) {
            drop.save(dropsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn a mob of this type at the specified location.
     * 
     * @param loc the location.
     * @return the mob.
     */
    public LivingEntity spawn(Location loc) {
        LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, _entityType);

        if (_babyFraction != null) {
            if (mob instanceof Ageable) {
                Ageable ageable = (Ageable) mob;
                ageable.setAdult();
                if (Math.random() < _babyFraction) {
                    ageable.setBaby();
                }
            }
        }

        AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (_speed != null) {
            speed.setBaseValue(_speed);
        }
        if (_health != null) {
            mob.setHealth(_health);
        }
        return mob;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this mob type.
     * 
     * @return the programmatic ID of this mob type.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the health of this mob type.
     * 
     * @param health the new health.
     */
    public void setHealth(Double health) {
        _health = health;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the speed of this mob type.
     * 
     * @param speed the new speed.
     */
    public void setSpeed(Double speed) {
        _speed = speed;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the fraction of mobs of this type that spawn as babies.
     * 
     * @param babyFraction the baby fraction, in the range [0.0, 1.0].
     */
    public void setBabyFraction(Double babyFraction) {
        _babyFraction = babyFraction;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a string description of this type.
     * 
     * @return a string description of this type.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(ChatColor.WHITE);
        desc.append("id: ");
        desc.append(ChatColor.YELLOW);
        desc.append(_id);
        desc.append(ChatColor.WHITE);
        desc.append(", entity-type: ");
        desc.append(ChatColor.YELLOW);
        desc.append(_entityType.toString());
        if (_babyFraction != null) {
            desc.append(ChatColor.WHITE);
            desc.append(", baby-fraction: ");
            desc.append(ChatColor.YELLOW);
            desc.append(_babyFraction);
        }
        if (_speed != null) {
            desc.append(ChatColor.WHITE);
            desc.append(", speed: ");
            desc.append(ChatColor.YELLOW);
            desc.append(_speed);
        }
        if (_health != null) {
            desc.append(ChatColor.WHITE);
            desc.append(", health: ");
            desc.append(ChatColor.YELLOW);
            desc.append(_health);
        }
        // TODO: potions.
        return desc.toString();
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
     * The programmatic ID of this MobType.
     */
    protected String _id;

    /**
     * The EntityType of the underlying mob.
     */
    protected EntityType _entityType;

    /**
     * Fraction of mobs of this type that are babies; null => no override.
     */
    protected Double _babyFraction;

    /**
     * Movement speed attribute value; null => no override.
     */
    protected Double _speed;

    /**
     * Health in hearts; null => no override.
     */
    protected Double _health;

    /**
     * Map from item ID to drop for this mob type.
     */
    protected HashMap<String, Drop> _drops = new HashMap<>();
} // class MobType