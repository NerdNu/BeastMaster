package nu.nerd.beastmaster.mobs;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import me.libraryaddict.disguise.utilities.parser.DisguiseParseException;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.commands.Commands;

// ----------------------------------------------------------------------------
/**
 * Concrete implementations of {@link IDataType}.
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
            return EntityType.valueOf(value);
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
                sender.sendMessage(ChatColor.RED + "Invalid disguise: " +
                                   (cause != null ? cause.getMessage() : ex.getMessage()));
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

} // class DataType