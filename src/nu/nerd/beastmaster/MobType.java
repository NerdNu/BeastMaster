package nu.nerd.beastmaster;

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
        // TODO: load loot table reference.
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
        // TODO: save loot table reference.
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

        // Don't change age of mob during configuration (which may happen
        // long after spawning).
        if (_babyFraction != null && mob instanceof Ageable) {
            Ageable ageable = (Ageable) mob;
            ageable.setAdult();
            if (Math.random() < _babyFraction) {
                ageable.setBaby();
            }
        }

        configureMob(mob);
        return mob;
    }

    // ------------------------------------------------------------------------
    /**
     * Configure a mob according to this mob type.
     * 
     * @param mob the mob.
     */
    public void configureMob(LivingEntity mob) {
        mob.setMetadata(BeastMaster.MOB_META_KEY, BeastMaster.MOB_META);
        AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (_speed != null) {
            speed.setBaseValue(_speed);
        }
        if (_health != null) {
            AttributeInstance maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            maxHealth.setBaseValue(_health);
            mob.setHealth(_health);
        }

        // Prevent mobs from picking up items, since MA is despawning them.
        mob.setCanPickupItems(false);

        // Clear default armour.
        mob.getEquipment().clear();
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
     * Return the EntityType of this mob type.
     * 
     * @return the EntityType of this mob type.
     */
    public EntityType getEntityType() {
        return _entityType;
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
        // TODO: show loot table reference.
        // TODO: potions.
        return desc.toString();
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

} // class MobType