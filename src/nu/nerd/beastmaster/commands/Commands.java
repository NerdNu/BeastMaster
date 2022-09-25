package nu.nerd.beastmaster.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

// ----------------------------------------------------------------------------
/**
 * Common code for implementing commands.
 */
public class Commands {
    // ------------------------------------------------------------------------
    /**
     * Parse a numerical command argument and return the boxed result of parsing
     * it, or return null if it could not be parsed or was out of range.
     *
     * @param valueArg    the command line argument to parse.
     * @param parse       a function that parses valueArg into a value of type T
     *                    or throws NumberFormatException.
     * @param inRange     a predicate that returns true if the parsed number is
     *                    in range.
     * @param rangeError  code to run if the parsed number is out of range.
     * @param formatError code to run if the argument cannot be parsed as a
     *                    number, such that a NumberFormatException was thrown.
     *                    If null, use run rangeError instead.
     * @return the parsed number, or null on error.
     */
    public static <T> T parseNumber(String valueArg,
                                    Function<String, T> parse,
                                    Predicate<T> inRange,
                                    Runnable rangeError,
                                    Runnable formatError) {
        try {
            T value = parse.apply(valueArg);
            if (inRange.test(value)) {
                return value;
            } else {
                rangeError.run();
                return null;
            }
        } catch (NumberFormatException ex) {
            Runnable formatHandler = (formatError != null) ? formatError : rangeError;
            formatHandler.run();
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a numerical command argument and return the result of parsing it in
     * an Optional<>, or return an empty Optional<> if the argument was a
     * distinguished value. If there is an error, return null instead of an
     * Optional<>.
     *
     * @param valueArg              the command line argument to parse.
     * @param distinguishedValueArg the distinguished value that causes an empty
     *                              Optional<> to be returned.
     * @param parse                 a function that parses valueArg into a value
     *                              of type T or throws NumberFormatException.
     * @param inRange               a predicate that returns true if the parsed
     *                              number is in range.
     * @param rangeError            code to run if the parsed number is out of
     *                              range.
     * @param formatError           code to run if the argument cannot be parsed
     *                              as a number, such that a
     *                              NumberFormatException was thrown. If null,
     *                              use run rangeError instead.
     * @return an Optional<T> containing the parsed number, or Optional<T>
     *         containing null if the valueArg is the distinguishedValueArg;
     *         return null (not an Optional<>) on error.
     */
    public static <T> Optional<T> parseNumberDefaulted(String valueArg,
                                                       String distinguishedValueArg,
                                                       Function<String, T> parse,
                                                       Predicate<T> inRange,
                                                       Runnable rangeError,
                                                       Runnable formatError) {
        if (valueArg.equals(distinguishedValueArg)) {
            return Optional.ofNullable(null);
        } else {
            T value = parseNumber(valueArg, parse, inRange, rangeError, formatError);
            return (value == null) ? null : Optional.of(value);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a string as a Double.
     *
     * The generics in here are to convince the compiler that T can be a Double.
     * Double::parseDouble doesn't cut it as a method reference when passed to
     * parseDefaulted().
     *
     * @param s the string.
     * @return a boxed double.
     * @throws NumberFormatException
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseDouble(String s) {
        return (T) new Double(Double.parseDouble(s));
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a string as a Float.
     *
     * The generics in here are to convince the compiler that T can be a Float.
     * Float::parseFloat doesn't cut it as a method reference when passed to
     * parseDefaulted().
     *
     * @param s the string.
     * @return a boxed double.
     * @throws NumberFormatException
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseFloat(String s) {
        return (T) new Float(Float.parseFloat(s));
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a string as an Integer.
     *
     * The generics in here are to convince the compiler that T can be an
     * Integer. Integer::parseInt doesn't cut it as a method reference when
     * passed to parseDefaulted().
     *
     * @param s the string.
     * @return a boxed int.
     * @throws NumberFormatException
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseInt(String s) {
        return (T) new Integer(Integer.parseInt(s));
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a Material, specified as either a case-insensitive name.
     *
     * @param sender      the ComamndSender.
     * @param materialArg the command argument containing the material name.
     * @return the Material
     */
    public static Material parseMaterial(CommandSender sender, String materialArg) {
        Material material = Material.getMaterial(materialArg.toUpperCase());
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "\"" + materialArg + "\" is not a valid material name.");
        }
        return material;
    }

    // ------------------------------------------------------------------------
    /**
     * Case insensitively parse a command argument as a boolean value (t/f,
     * true/false, y/n, yes/no, on/off).
     *
     * @param sender  the CommandSender.
     * @param arg     the command argument to parse as boolean.
     * @param argName a name identifying the purpose of the argument, to be
     *                shown in error messages.
     * @return true or false, respectively, for values that are unambiguously
     *         true or false, and null for everything else.
     */
    public static Boolean parseBoolean(CommandSender sender, String arg, String argName) {
        String lowerArg = arg.toLowerCase();
        if (TRUE_STRINGS.contains(lowerArg)) {
            return true;
        } else if (FALSE_STRINGS.contains(lowerArg)) {
            return false;
        } else {
            sender.sendMessage(ChatColor.RED + "\"" + arg + "\" is not a valid " + argName + " argument.");
            sender.sendMessage(ChatColor.RED + "Valid values are: yes/no/y/n/true/false/t/f/on/off");
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a predicate that is true if its argument is greater than or min
     * and less than or equal to max.
     *
     * If a bound is specified as null, it is not enforced.
     *
     * @param min the optional lower bound.
     * @param max the optional upper bound.
     * @return the predicate.
     */
    public static Predicate<Double> inclusiveRangePredicate(Double min, Double max) {
        return x -> (min == null || x >= min) &&
                    (max == null || x <= max);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a predicate that is true if its argument is greater than or min
     * and less than or equal to max.
     *
     * If a bound is specified as null, it is not enforced.
     *
     * @param min the optional lower bound.
     * @param max the optional upper bound.
     * @return the predicate.
     */
    public static Predicate<Integer> inclusiveRangePredicate(Integer min, Integer max) {
        return x -> (min == null || x >= min) &&
                    (max == null || x <= max);
    }

    // ------------------------------------------------------------------------
    /**
     * Show the standard error message for when a thing is null and should not
     * be.
     *
     * @param sender           the command sender.
     * @param thingDescription a human-readable description of the thing.
     * @param thingId          the ID used to look up the thing.
     */
    public static void errorNull(CommandSender sender, String thingDescription, String thingId) {
        sender.sendMessage(ChatColor.RED + "There is no " + thingDescription + " named \"" + thingId + "\"!");
    }

    // ------------------------------------------------------------------------
    /**
     * Show the standard error message for when a thing is not null, but should
     * be.
     *
     * @param sender           the command sender.
     * @param thingDescription a human-readable description of the thing.
     * @param thingId          the ID used to look up the thing.
     */
    public static void errorNotNull(CommandSender sender, String thingDescription, String thingId) {
        String vowels = "aeiou";
        String firstLetter = "" + Character.toLowerCase(thingDescription.charAt(0));
        String firstWord = vowels.contains(firstLetter) ? "An " : "A ";
        sender.sendMessage(ChatColor.RED + firstWord + thingDescription + " named " + thingId + " already exists!");
    }

    // ------------------------------------------------------------------------
    /**
     * Show the standard message for invalid arguments.
     *
     * @param usage the correct usage of the command, omitting the leading "/".
     * @return true if the argument count was correct.
     */
    public static void invalidArguments(CommandSender sender, String usage) {
        sender.sendMessage(ChatColor.RED + "Invalid arguments.");
        sender.sendMessage(ChatColor.RED + "Usage: /" + usage);
    }

    // ------------------------------------------------------------------------
    /**
     * Return an error message to be presented to the user signifying that a
     * numeric value must be in the inclusive range specified by min and max.
     *
     * @param thing the name of the value to be constrained, e.g. "value".
     * @param type  the name of the type of the value, e.g. "an integer", "a
     *              real number".
     * @param min   the lower bound of the range, or null if not constrained.
     * @param max   the upper bound of the range, or null if not constrained.
     * @return an error message.
     */
    public static <T> String rangeErrorMessage(String thing, String type, T min, T max) {
        StringBuilder message = new StringBuilder();
        message.append("The ").append(thing);
        message.append(" must be ").append(type);
        if (min != null) {
            if (max != null) {
                message.append(" in the range ");
                message.append(min).append(" to ").append(max);
                message.append(", inclusive.");
            } else {
                // min but no max.
                message.append(" not less than ").append(min).append('.');
            }
        } else {
            // max but no min.
            if (max != null) {
                message.append(" not greater than ").append(max);
            }
            message.append('.');
        }
        return message.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Send the specified error message through the sender, or log to the
     * console as an error if the CommandSender is null.
     *
     * @param sender  the command sender.
     * @param message the message, including all chat formatting.
     */
    public static void sendError(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            Bukkit.getLogger().severe(message);
        }

    }

    // ------------------------------------------------------------------------
    /**
     * String values treated as false.
     */
    private static List<String> FALSE_STRINGS = Arrays.asList("f", "false", "n", "no", "off");

    /**
     * String values treated as true.
     */
    private static List<String> TRUE_STRINGS = Arrays.asList("t", "true", "y", "yes", "on");
} // class Commands