package nu.nerd.beastmaster.commands;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropType;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.mobs.MobProperty;
import nu.nerd.beastmaster.mobs.MobType;

// ----------------------------------------------------------------------------
/**
 * Executor for the /beast-mob command.
 */
public class BeastMobExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastMobExecutor() {
        super("beast-mob", "help",
              "add", "remove", "list",
              "info", "parent",
              "get", "set", "clear");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
            return false;
        }

        if (args.length >= 1) {
            if (args[0].equals("add")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " add <mob-id> <parent-id>");
                    return true;
                }

                String idArg = args[1];
                String parentIdArg = args[2];

                if (DropType.isDropType(idArg)) {
                    sender.sendMessage(ChatColor.RED + "You can't use The ID \"" + idArg + "\"; it is reserved.");
                    return true;
                }

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType != null) {
                    Commands.errorNotNull(sender, "mob type", idArg);
                    return true;
                }

                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item != null) {
                    sender.sendMessage(ChatColor.RED + "An item named \"" + idArg + "\" is already defined. " +
                                       "Mob types can't have the same ID as an existing item.");
                    return true;
                }

                MobType parentType = BeastMaster.MOBS.getMobType(parentIdArg);
                if (parentType == null) {
                    Commands.errorNull(sender, "parent type", idArg);
                    return true;
                }

                mobType = new MobType(idArg, parentIdArg);
                BeastMaster.MOBS.addMobType(mobType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added a new mob type: " + mobType.getShortDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <mob-id>");
                    return true;
                }

                String mobIdArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                BeastMaster.MOBS.removeMobType(mobType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed mob type: " + mobType.getShortDescription());
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Predefined mob types: " +
                                   BeastMaster.MOBS.getPredefinedMobTypes().stream()
                                   .map(MobType::getShortDescription)
                                   .collect(Collectors.joining(ChatColor.WHITE + ", ")));

                sender.sendMessage(ChatColor.GOLD + "Custom mob types:");
                for (MobType mobType : BeastMaster.MOBS.getAllMobTypes()) {
                    if (!mobType.isPredefined()) {
                        sender.sendMessage(mobType.getShortDescription());
                    }
                }
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <mob-id>");
                    return true;
                }

                String mobIdArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Mob type: " + ChatColor.YELLOW + mobType.getId());
                for (String propertyId : MobType.getAllPropertyIds()) {
                    showProperty(sender, mobType, propertyId, false);
                }
                return true;

            } else if (args[0].equals("get")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " get <mob-id> <property>");
                    listPropertyIds(sender);
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    listPropertyIds(sender);
                    return true;
                }

                showProperty(sender, mobType, propertyArg, true);
                return true;

            } else if (args[0].equals("set")) {
                if (args.length < 4) {
                    Commands.invalidArguments(sender, getName() + " set <mob-id> <property> <value>");
                    listPropertyIds(sender);
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];
                String valueArg = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                if (mobType.isPredefined() &&
                    MobType.getImmutablePredefinedPropertyNames().contains(propertyArg)) {
                    sender.sendMessage(ChatColor.RED + "You cannot change the " + propertyArg +
                                       " property of predefined mob types.");
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    listPropertyIds(sender);
                    return true;
                }

                Object value;
                try {
                    value = property.getType().parse(valueArg);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + valueArg + " is not a valid value.");
                    return true;
                }

                property.setValue(value);
                sender.sendMessage(ChatColor.GOLD + mobType.getId() + ": " +
                                   ChatColor.YELLOW + property.getId() +
                                   ChatColor.WHITE + " = " +
                                   ChatColor.YELLOW + property.getFormattedValue());
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("clear")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " clear <mob-id> <property>");
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    return true;
                }

                property.setValue(null);
                sender.sendMessage(ChatColor.GOLD + mobType.getId() + ": " +
                                   ChatColor.YELLOW + property.getId() +
                                   ChatColor.WHITE + " = " +
                                   ChatColor.YELLOW + "unset");
                BeastMaster.CONFIG.save();
                return true;
            }
        }

        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * List all valid property IDs.
     * 
     * @param sender the command sender to message.
     */
    protected void listPropertyIds(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Valid property names are: " +
                           MobType.getAllPropertyIds().stream()
                           .map(id -> ChatColor.YELLOW + id)
                           .collect(Collectors.joining(ChatColor.WHITE + ", ")));
    }

    // ------------------------------------------------------------------------
    /**
     * Message the CommandSender the value of the specified mob property.
     * 
     * Where the property is unset, the inherited value is shown and the
     * ancestor MobType from which the property value was inherited is
     * indicated.
     * 
     * @param sender the command sender.
     * @param mobType the type of the mob.
     * @param propertyId the ID of the property whose value is shown.
     * @param showUnset if true, show the values of properties that have not
     *        been set (are null); otherwise don't show those.
     */
    protected void showProperty(CommandSender sender, MobType mobType,
                                String propertyId, boolean showUnset) {
        MobProperty property = mobType.getProperty(propertyId);
        MobProperty derivedProperty = mobType.getDerivedProperty(propertyId);

        if (derivedProperty.getValue() != null || showUnset) {
            // Show source of inherited properties only.
            String source = (property != derivedProperty) ? derivedProperty.getMobType().getId() + ": "
                                                          : "";
            sender.sendMessage(ChatColor.GOLD + property.getId() + ": " +
                               ChatColor.WHITE + source +
                               ChatColor.YELLOW + derivedProperty.getFormattedValue());
        }
    }
} // class BeastMobExecutor