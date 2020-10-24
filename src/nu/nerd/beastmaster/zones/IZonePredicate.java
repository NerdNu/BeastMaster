package nu.nerd.beastmaster.zones;

import java.util.List;

import org.bukkit.Location;

// ----------------------------------------------------------------------------
/**
 * Validates the constraints on and evaluates a Zone Specification predicate.
 * 
 * Constraints are validated when the predicate is parsed; for example, in
 * circle(x,z,radius) to check that radius >= 0.
 */
public interface IZonePredicate {
    // ------------------------------------------------------------------------
    /**
     * Validate at compile time that the arguments to a zone predicate conform
     * to all value constraints.
     * 
     * @param argTokens the predicate arguments as Tokens.
     * @param args the values of the arguments at evaluation time (can be
     *        modified).
     * @throws ParseError if type or value constraints are violated.
     */
    public void validateArgs(List<Token> argTokens, List<Object> args);

    // ------------------------------------------------------------------------
    /**
     * Return true if the predicate matches the specified Location.
     * 
     * @param loc the Location.
     * @param args the predicate arguments (of type String and Double, only).
     * @return the value of the predicate at the specifeid Location.
     */
    public boolean matches(Location loc, List<Object> args);
} // class IZonePredicate