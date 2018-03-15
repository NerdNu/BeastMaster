package nu.nerd.beastmaster.mobs;

import java.util.Comparator;

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
    default public String format(Object value) {
        return value.toString();
    }

    public Object parse(String value) throws IllegalArgumentException;

    default public String serialise(Object value) {
        return value.toString();
    }

    public Object deserialise(String value) throws IllegalArgumentException;

} // class IDataType