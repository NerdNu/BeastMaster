package nu.nerd.beastmaster.mobs;

import java.util.Comparator;

import org.bukkit.command.CommandSender;

// ----------------------------------------------------------------------------
/**
 * Defines data types as methods to support the string representation of a
 * property value in the command line interface and in serialised YAML data.
 *
 * The serialisation format encodes null as the absence of a property in the
 * YAML file, rather than a distinguished value. Data types are only required to
 * support boxed Java primitive types and strings. More complex structures are
 * manipulated by orthogonal, dedicated facilities, such as the
 * {@code /beast-item} command, and referenced by their string ID.
 */
public interface IDataType extends Comparator<Object> {
    // ------------------------------------------------------------------------
    /**
     * Format the value as appropriate to this data type.
     *
     * @param value the value.
     * @return the formatted value.
     */
    default public String format(Object value) {
        return value.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Parse the value from an interactively input string.
     *
     * @param value  the string to parse.
     * @param sender the command sender to report errors to, or null when
     *               loading configuration.
     * @param id     the property ID being parsed.
     * @return null if the parse() implementation sends its own error message
     *         via the CommandSender, or throw an IllegalArgumentException to
     *         elicit a default error message.
     */
    public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException;

    // ------------------------------------------------------------------------
    /***
     * Serialise the value for storage in the configuration.
     *
     * @param value the value to store.
     * @return the string to store in the config.
     */
    default public String serialise(Object value) {
        return value.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Deserialise the value from a string retrieved from the configuration.
     *
     * @param value the string to deserialise.
     * @return the corrsponding object.
     */
    public Object deserialise(String value) throws IllegalArgumentException;

} // class IDataType