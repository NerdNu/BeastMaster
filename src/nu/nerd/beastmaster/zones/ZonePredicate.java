package nu.nerd.beastmaster.zones;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Biome;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import nu.nerd.beastmaster.Util;

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
                return loc.getBlock().getBiome() == args.get(0);
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
                double x = (Double) args.get(0);
                double z = (Double) args.get(1);
                double radius = (Double) args.get(2);
                double dx = loc.getX() - x;
                double dz = loc.getZ() - z;
                return dx * dx + dz * dz <= radius * radius;
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
                double x = (Double) args.get(0);
                double z = (Double) args.get(1);
                double min = (Double) args.get(2);
                double max = (Double) args.get(3);
                double dx = loc.getX() - x;
                double dz = loc.getZ() - z;
                double distSquared = dx * dx + dz * dz;
                return distSquared >= min * min && distSquared <= max * max;
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
                double x1 = (Double) args.get(0);
                double z1 = (Double) args.get(1);
                double x2 = (Double) args.get(2);
                double z2 = (Double) args.get(3);
                return Util.inRect(loc.getX(), loc.getZ(), x1, z1, x2, z2);
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
                double x = (Double) args.get(0);
                double z = (Double) args.get(1);
                double side = (Double) args.get(2);
                double r = side / 2;
                return Util.inRect(loc.getX(), loc.getZ(), x - r, z - r, x + r, z + r);
            }
        }),

    WG(
        "Location is within the WorldGuard region of the specified name, or \"*\" to match any region.",
        new ZonePredicateParameters("name", String.class),
        new IZonePredicate() {
            @Override
            public void validateArgs(List<Token> argTokens, List<Object> args) {
            }

            @Override
            public boolean matches(Location loc, List<Object> args) {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionManager regionManager = container.get(BukkitAdapter.adapt(loc.getWorld()));
                if (regionManager != null) {
                    com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(loc);
                    // applicableRegions does NOT include the global region.
                    ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(weLoc.toVector().toBlockPoint());
                    String name = (String) args.get(0);
                    if (applicableRegions.size() == 0) {
                        if (name.equalsIgnoreCase("__global__")) {
                            return true;
                        }
                    } else { // There are non-global regions.
                        if (name.equals("*")) {
                            // Any (non-global) region will do.
                            return true;
                        }

                        for (ProtectedRegion region : applicableRegions) {
                            if (name.equals(region.getId())) {
                                return true;
                            }
                        }
                    }
                }
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
                double max = (Double) args.get(1);
                if (max <= min) {
                    throw new ParseError("the maximum Y must be greater than the minimum Y", argTokens.get(1));
                }
            }

            @Override
            public boolean matches(Location loc, List<Object> args) {
                double min = (Double) args.get(0);
                double max = (Double) args.get(1);
                return min <= loc.getY() && loc.getY() <= max;
            }
        })

    // TODO: Implement this predicate: need to guard against infinite (direct or
    // indirect) recursion.
//    ,ZONE("Location is within the zone of the specified name.",
//        new ZonePredicateParameters("name", String.class),
//        new IZonePredicate() {
//            @Override
//            public void validateArgs(List<Token> argTokens, List<Object> args) {
//            }
//
//            @Override
//            public boolean matches(Location loc, List<Object> args) {
//                return false;
//            }
//        })

    ;

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
     * @param args      the values of the arguments at evaluation time (can be
     *                  modified).
     * @throws ParseError if type or value constraints are violated.
     */
    public void validateArgs(List<Token> argTokens, List<Object> args) {
        _zonePredicate.validateArgs(argTokens, args);
    }

    // ------------------------------------------------------------------------
    /**
     * Evaluate the predicate at the specified location.
     *
     * @param loc  the Location.
     * @param args the predicate arguments.
     * @return true if the predicate matches.
     */
    public boolean matches(Location loc, List<Object> args) {
        return _zonePredicate.matches(loc, args);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param help       help text describing the predicate.
     * @param parameters describes the formal paramters of the predicate.
     * @param evaluate   evaluates the predicate.
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
