package nu.nerd.beastmaster.mobs;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;

// ----------------------------------------------------------------------------
/**
 * An interface implemented by all configurable properties of mobs.
 * 
 * TODO: custom formatting function per property instance.
 */
public class MobProperty {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the ID of the property.
     * @param type the type of the property; defines formatting, parsing and
     *        configuration persistence.
     * @param configureMob the code to execute to configure this property on a
     *        mob.
     */
    public MobProperty(String id, IDataType type, BiConsumer<Creature, Logger> configureMob) {
        _id = id;
        _type = type;
        _configureMob = configureMob;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the mob type that owns this property.
     * 
     * @param mobType the mob type.
     */
    public void setMobType(MobType mobType) {
        _mobType = mobType;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the mob type that owns this property.
     * 
     * @return the mob type that owns this property.
     */
    public MobType getMobType() {
        return _mobType;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the identifier of this property.
     * 
     * @return the identifier of this property.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the type of this property.
     * 
     * @return the type of this property.
     */
    public IDataType getType() {
        return _type;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this property can be null.
     * 
     * @return true if this property can be null.
     */
    public boolean isNullable() {
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the key under which this property is saved to the configuration.
     * 
     * @return the key under which this property is saved to the configuration.
     */
    public String getConfigurationKey() {
        return getId();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the value of this property formatted for presentation to the user.
     * 
     * @return the value of this property formatted for presentation to the
     *         user.
     */
    public String getFormattedValue() {
        return getValue() != null ? _type.format(getValue()) : "unset";
    }

    // ------------------------------------------------------------------------
    /**
     * Set the value of this property.
     * 
     * @param value the value.
     */
    public void setValue(Object value) {
        _value = value;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the value of this property in its natural type representation, or
     * null to signify that the property does not override the parent's default.
     * 
     * @return the value of this property in its natural type representation, or
     *         null to signify that the property does not override the parent's
     *         default.
     */
    public Object getValue() {
        return _value;
    }

    // ------------------------------------------------------------------------
    /**
     * Configure a mob according to this property.
     * 
     * @param mob the mob.
     * @param logger used to log errors or warnings.
     */
    public void configureMob(Creature mob, Logger logger) {
        _configureMob.accept(mob, logger);
    }

    // ------------------------------------------------------------------------
    /**
     * Load this property from the specified ConfigurationSection.
     * 
     * @param section the section.
     * @param logger used to log messages.
     */
    public void load(ConfigurationSection section, Logger logger) {
        String serialised = section.getString(getConfigurationKey());
        try {
            setValue(serialised == null ? null : _type.deserialise(serialised));
        } catch (IllegalArgumentException ex) {
            logger.severe("error deserialising property " +
                          section.getName() + "." + getConfigurationKey());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save this property to the specified ConfigurationSection.
     * 
     * @param section the section.
     * @param logger used to log messages.
     */
    public void save(ConfigurationSection section, Logger logger) {
        if (getValue() != null) {
            section.set(getConfigurationKey(), _type.serialise(getValue()));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The ID of this property.
     */
    protected String _id;

    /**
     * The type of this property.
     */
    protected IDataType _type;

    /**
     * The value of this property.
     */
    protected Object _value;

    /**
     * The code to execute to configure this property on a mob.
     */
    protected BiConsumer<Creature, Logger> _configureMob;

    /**
     * The mob type that owns the property.
     */
    protected MobType _mobType;

} // class MobProperty