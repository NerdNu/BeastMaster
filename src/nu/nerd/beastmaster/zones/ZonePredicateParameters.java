package nu.nerd.beastmaster.zones;

import java.util.ArrayList;
import java.util.List;

// ----------------------------------------------------------------------------
/**
 * Records the names and types of the parameters of a Zone Specification
 * predicate, and validates them.
 */
public class ZonePredicateParameters {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param parameters the parameter list as name1, type1, name2, type2, etc.
     *        where names are strings and types are either String.class or
     *        Double.class.
     */
    public ZonePredicateParameters(Object... parameters) {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("parameters must be specified as alternating names, then types");
        }

        // Partition parameters into names and types.
        for (int i = 0; i < parameters.length / 2; ++i) {
            _names.add((String) parameters[2 * i]);
            _types.add((Class<?>) parameters[2 * i + 1]);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the number of parameters.
     * 
     * @return the number of parameters.
     */
    public int size() {
        return _names.size();
    }

    // ------------------------------------------------------------------------
    /**
     * Verify that the types of the arguments to a predicate are as expected.
     * 
     * @param argTokens the predicate arguments as Tokens.
     * @throws ParseError if arguments have the wrong types.
     */
    public void validateTypes(List<Token> argTokens) {
        for (int i = 0; i < argTokens.size(); ++i) {
            Class<?> expectedType = _types.get(i);
            Token token = argTokens.get(i);
            Object value = token.getValue();
            if (!expectedType.isInstance(value)) {
                throw new ParseError("argument " + (i + 1) +
                                     " " + readableTypeName(expectedType) +
                                     " not a " + readableTypeName(value.getClass()),
                    token);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the name of a type as it should be presented to the user.
     * 
     * @param type the type of an argument or parameter.
     * @return the name of a type as it should be presented to the user.
     */
    protected static String readableTypeName(Class<?> type) {
        return (type == Double.class) ? "number" : "string";
    }

    // ------------------------------------------------------------------------
    /**
     * The names of the paramaters.
     */
    protected List<String> _names = new ArrayList<>();

    /**
     * The corresponding types of the paramaters.
     */
    protected List<Class<?>> _types = new ArrayList<>();

} // class ZonePredicateParameters