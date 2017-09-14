package nu.nerd.beastmaster.commands;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
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
     * @param valueArg the command line argument to parse.
     * @param parse a function that parses valueArg into a value of type T or
     *        throws NumberFormatException.
     * @param inRange a predicate that returns true if the parsed number is in
     *        range.
     * @param rangeError code to run if the parsed number is out of range.
     * @param formatError code to run if the argument cannot be parsed as a
     *        number, such that a NumberFormatException was thrown. If null, use
     *        run rangeError instead.
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
     * @param valueArg the command line argument to parse.
     * @param distinguishedValueArg the distinguished value that causes an empty
     *        Optional<> to be returned.
     * @param parse a function that parses valueArg into a value of type T or
     *        throws NumberFormatException.
     * @param inRange a predicate that returns true if the parsed number is in
     *        range.
     * @param rangeError code to run if the parsed number is out of range.
     * @param formatError code to run if the argument cannot be parsed as a
     *        number, such that a NumberFormatException was thrown. If null, use
     *        run rangeError instead.
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
     * @param sender the command sender.
     * @param thingDescription a human-readable description of the thing.
     * @param thingId the ID used to look up the thing.
     */
    public static void errorNull(CommandSender sender, String thingDescription, String thingId) {
        sender.sendMessage(ChatColor.RED + "There is no " + thingDescription + " named \"" + thingId + "\"!");
    }

    // ------------------------------------------------------------------------
    /**
     * Show the standard error message for when a thing is not null, but should
     * be.
     * 
     * @param sender the command sender.
     * @param thingDescription a human-readable description of the thing.
     * @param thingId the ID used to look up the thing.
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
        sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + usage);
    }

    // ------------------------------------------------------------------------
} // class Commands