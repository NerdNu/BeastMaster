package nu.nerd.beastmaster.zones;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Biome;

// ----------------------------------------------------------------------------
/**
 * An enumeration of all Zone Specification predicates.
 */
public enum ZonePredicate {
    BIOME(
    "Location is in the specified biome type.",
    new ZonePredicateParameters("type", String.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {
            String biomeName = ((String) args.get(0)).toUpperCase();
            try {
                args.set(0, Biome.valueOf(biomeName));
            } catch (IllegalArgumentException ex) {
                throw new ParseError("invalid biome name: " + biomeName +
                                     "; it should be a double-quoted Biome API constant",
                    argTokens.get(0));
            }
        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    CIRCLE(
    "Location is within radius blocks of (x,z).",
    new ZonePredicateParameters("x", Double.class, "z", Double.class, "radius", Double.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {
            double radius = (Double) args.get(2);
            if (radius <= 0.0) {
                throw new ParseError("the radius must be greater than 0", argTokens.get(2));
            }
        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    DONUT(
    "Location is between min and max blocks of (x,z).",
    new ZonePredicateParameters("x", Double.class, "z", Double.class, "min", Double.class, "max", Double.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {
            double min = (Double) args.get(2);
            if (min <= 0.0) {
                throw new ParseError("the minimum radius must be greater than 0", argTokens.get(2));
            }
            double max = (Double) args.get(3);
            if (max <= min) {
                throw new ParseError("the maximum radius must be greater than the minimum radius", argTokens.get(3));
            }
        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    RECT(
    "Location is within the rectangle (x1,z1) to (x2,z2).",
    new ZonePredicateParameters("x1", Double.class, "z1", Double.class, "x2", Double.class, "z2", Double.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {

        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    SQUARE(
    "Location is within the square of specified side length centred on (x,z).",
    new ZonePredicateParameters("x", Double.class, "z", Double.class, "side", Double.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {
            double side = (Double) args.get(2);
            if (side <= 0.0) {
                throw new ParseError("the side must be greater than 0", argTokens.get(2));
            }
        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    WG(
    "Location is within the WorldGuard region of the specified name.",
    new ZonePredicateParameters("name", String.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {

        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    Y(
    "Location Y coordinate is within the range [min,max].",
    new ZonePredicateParameters("min", Double.class, "max", Double.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {
            double min = (Double) args.get(0);
            if (min <= 0.0) {
                throw new ParseError("the minimum Y must be greater than 0", argTokens.get(0));
            }
            double max = (Double) args.get(1);
            if (max <= min) {
                throw new ParseError("the maximum Y must be greater than the minimum Y", argTokens.get(1));
            }
        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    }),

    ZONE("Location is within the zone of the specified name.",
    new ZonePredicateParameters("name", String.class),
    new IZonePredicate() {
        @Override
        public void validateArgs(List<Token> argTokens, List<Object> args) {

        }

        @Override
        public boolean matches(Location loc, List<Object> args) {
            return false;
        }
    });

    // ------------------------------------------------------------------------
    /**
     * Return the ZonePredicate with the specified (case-insensitive)
     * identifier, or null if none matches.
     * 
     * @param ident the identifier of the predicate.
     * @return the ZonePredicate with the specified (case-insensitive)
     *         identifier, or null if none matches.
     */
    public static ZonePredicate byIdent(String ident) {
        try {
            return valueOf(ident.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the help text describing the predicate.
     * 
     * @return the help text describing the predicate.
     */
    public String getHelp() {
        return _help;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the parameters of this Zone Specification predicate.
     * 
     * @return the parameters of this Zone Specification predicate.
     */
    public ZonePredicateParameters getParameters() {
        return _parameters;
    }

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
    public void validateArgs(List<Token> argTokens, List<Object> args) {
        _zonePredicate.validateArgs(argTokens, args);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param help help text describing the predicate.
     * @param parameters describes the formal paramters of the predicate.
     * @param evaluate evaluates the predicate.
     */
    ZonePredicate(String help, ZonePredicateParameters parameters, IZonePredicate zonePredicate) {
        _help = help;
        _parameters = parameters;
        _zonePredicate = zonePredicate;
    }

    // ------------------------------------------------------------------------
    /**
     * The help text describing the predicate.
     */
    protected String _help;

    /**
     * Describes the formal parameters of the predicate.
     */
    protected ZonePredicateParameters _parameters;

    /**
     * Validates constraints and evaluates the predicate.
     */
    protected IZonePredicate _zonePredicate;

} // class ZonePredicate
