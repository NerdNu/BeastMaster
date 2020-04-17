package nu.nerd.beastmaster.mobs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import me.libraryaddict.disguise.utilities.parser.DisguiseParseException;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.SoundEffect;
import nu.nerd.beastmaster.commands.Commands;

// ----------------------------------------------------------------------------
/**
 * Concrete implementations of {@link IDataType}.
 * 
 * TODO: audit where compare() is being called and work out whether I actually
 * need to handle null.
 */
public class DataType {
    // ------------------------------------------------------------------------

    public static final IDataType BOOLEAN = new IDataType() {
        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return Commands.parseBoolean(sender, value, id);
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return Boolean.parseBoolean(value);
        }

        @Override
        public int compare(Object o1, Object o2) {
            return Boolean.compare((Boolean) o1, (Boolean) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType INTEGER = new IDataType() {
        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return Integer.parseInt(value);
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return Integer.parseInt(value);
        }

        @Override
        public int compare(Object o1, Object o2) {
            return Integer.compare((Integer) o1, (Integer) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType DOUBLE = new IDataType() {
        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return Double.parseDouble(value);
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return Double.parseDouble(value);
        }

        @Override
        public int compare(Object o1, Object o2) {
            return Double.compare((Double) o1, (Double) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType STRING = new IDataType() {
        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return value;
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------
    /**
     * Comma separated ordered set of case-insensitive Strings.
     * 
     * Spaces are treated as equivalent to commas.
     */
    public static final IDataType TAG_SET = new IDataType() {
        @SuppressWarnings("unchecked")
        @Override
        public String format(Object value) {
            return ((Set<String>) value).stream().collect(Collectors.joining(","));
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return deserialise(value.replaceAll("\\s+", ",").replaceAll(",+", ","));
        }

        @Override
        public String serialise(Object value) {
            return format(value);
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            // Handle input like: "," by removing empty tags.
            List<String> tags = Arrays.asList(value.split(",")).stream()
            .filter(tag -> !tag.isEmpty())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
            set.addAll(tags);
            return set;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return serialise(o1).compareToIgnoreCase(serialise(o2));
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType LOOT = new IDataType() {
        @Override
        public String format(Object value) {
            String id = (String) value;
            ChatColor colour = (BeastMaster.LOOTS.getDropSet(id) != null) ? ChatColor.GREEN : ChatColor.RED;
            return colour + id;
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return value;
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType LOOT_OR_ITEM = new IDataType() {
        @Override
        public String format(Object value) {
            String id = (String) value;
            if (BeastMaster.LOOTS.getDropSet(id) != null) {
                return ChatColor.GREEN + id + ChatColor.WHITE + " (loot)";
            } else if (BeastMaster.ITEMS.getItem(id) != null) {
                return ChatColor.GREEN + id + ChatColor.WHITE + " (item)";
            } else {
                return ChatColor.RED + id;
            }
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            // Canonicalise the case of loot table and item names.
            DropSet dropSet = BeastMaster.LOOTS.getDropSet(value);
            if (dropSet != null) {
                return dropSet.getId();
            }

            Item item = BeastMaster.ITEMS.getItem(value);
            if (item != null) {
                return item.getId();
            }

            return value;
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType LOOT_OR_MOB = new IDataType() {
        @Override
        public String format(Object value) {
            String id = (String) value;
            if (BeastMaster.LOOTS.getDropSet(id) != null) {
                return ChatColor.GREEN + id + ChatColor.WHITE + " (loot)";
            } else if (BeastMaster.MOBS.getMobType(id) != null) {
                return ChatColor.GREEN + id + ChatColor.WHITE + " (mob)";
            } else {
                return ChatColor.RED + id;
            }
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return value;
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType POTION_SET = new IDataType() {
        @Override
        public String format(Object value) {
            String id = (String) value;
            ChatColor colour = (BeastMaster.POTIONS.getPotionSet(id) != null) ? ChatColor.GREEN : ChatColor.RED;
            return colour + id;
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return value;
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType ENTITY_TYPE = new IDataType() {
        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            return EntityType.valueOf(value.toUpperCase());
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return EntityType.valueOf(value);
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((EntityType) o1).compareTo((EntityType) o2);
        }
    };

    // ------------------------------------------------------------------------

    public static final IDataType DISGUISE = new IDataType() {
        @Override
        public String format(Object value) {
            try {
                DisguiseParser.parseDisguise(Bukkit.getConsoleSender(), null, (String) value);
                return ChatColor.GREEN + value.toString();
            } catch (Exception ex) {
                return ChatColor.RED + value.toString();
            }
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            try {
                DisguiseParser.parseDisguise(Bukkit.getConsoleSender(), null, value);
                return value;
            } catch (IllegalAccessException | InvocationTargetException | DisguiseParseException ex) {
                Throwable cause = ex.getCause();
                sender.sendMessage(ChatColor.RED + "Invalid disguise: " + (cause != null ? cause.getMessage() : ex.getMessage()));
                return null;
            }
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return value;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    // ------------------------------------------------------------------------
    /**
     * IDataType for properties of the {@link SoundEffect}.
     * 
     * TODO: if /beast-tune is added, support tune ID as higher precendence.
     */
    public static final IDataType SOUND_EFFECT = new IDataType() {
        @Override
        public String format(Object value) {
            return ChatColor.GREEN + ((SoundEffect) value).toString();
        }

        @Override
        public Object parse(String value, CommandSender sender, String id) throws IllegalArgumentException {
            String[] args = value.trim().split("\\s+");
            if (args.length < 1 || args.length > 3) {
                showHelp(sender, value, -1);
                return null;
            }

            Sound sound = null;
            if (args.length >= 1) {
                String typeArg = args[0];
                try {
                    sound = Sound.valueOf(typeArg.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    if (sender != null) {
                        sender.sendMessage(ChatColor.RED + "Invalid sound type: " + typeArg);
                    }
                    showHelp(sender, value, 0);
                    return null;
                }
            }

            Double rangeMetres = null;
            if (args.length >= 2) {
                String rangeArg = args[1];
                boolean valid = false;
                try {
                    rangeMetres = Math.abs(Double.parseDouble(rangeArg));
                    if (rangeMetres > 0.0) {
                        valid = true;
                    }
                } catch (IllegalArgumentException ex) {
                }
                if (!valid) {
                    if (sender != null) {
                        sender.sendMessage(ChatColor.RED + "The range must be a positive number!");
                    }
                    showHelp(sender, value, 1);
                    return null;
                }
            }

            // Default pitch null is the same as "random".
            Double pitch = null;
            if (args.length == 3) {
                String pitchArg = args[2];
                boolean valid = false;
                try {
                    if (pitchArg.equalsIgnoreCase("random")) {
                        valid = true;
                    } else {
                        pitch = Double.parseDouble(pitchArg);
                        if (pitch >= 0.5 && pitch <= 2.0) {
                            valid = true;
                        }
                    }
                } catch (IllegalArgumentException ex) {
                }
                if (!valid) {
                    if (sender != null) {
                        sender.sendMessage(ChatColor.RED + "The pitch must be in the range 0.5 to 2.0!");
                    }
                    showHelp(sender, value, 2);
                    return null;
                }
            }
            return new SoundEffect(sound, rangeMetres, pitch);
        }

        @Override
        public Object deserialise(String value) throws IllegalArgumentException {
            return parse(value, null, null);
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ((SoundEffect) o1).compareTo((SoundEffect) o2);
        }

        /**
         * Show a help message when parsing sounds.
         * 
         * When loading config, parse() is called with a null command sender. We
         * log an error in console instead.
         * 
         * @param sender the sender to send the message to.
         * @param value the string that was parsed as a SoundEffect.
         * @param parsedSoFar is the number of fields successfully parsed, or -1
         *        for invalid argument count.
         */
        protected void showHelp(CommandSender sender, String value, int parsedSoFar) {
            if (sender == null) {
                BeastMaster.PLUGIN.getLogger().warning("Invalid sound read from config: \"" + value + "\", error code " + parsedSoFar);
                return;
            }
            if (parsedSoFar == -1 || parsedSoFar == 0) {
                sender.sendMessage(ChatColor.RED + "Specify sound as: <type> [<range>] [<pitch>|random]");
                sender.sendMessage(ChatColor.GOLD + "The type must be one of the constants at: " +
                                   ChatColor.AQUA + ChatColor.UNDERLINE + "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html");
            }
            if (parsedSoFar == -1 || parsedSoFar == 1) {
                sender.sendMessage(ChatColor.GOLD + "<range>, if specified, is the audible range in metres and defaults to 15 if omitted.");
            }
            if (parsedSoFar == -1 || parsedSoFar == 2) {
                sender.sendMessage(ChatColor.GOLD + "<pitch>, if specified, is the playback speed from 0.5 to 2.0, with 1.0 being normal speed.");
                sender.sendMessage(ChatColor.GOLD + "<pitch> can also be specified as the word 'random' to be chosen at playback time.");
            }
        }
    };
} // class DataType