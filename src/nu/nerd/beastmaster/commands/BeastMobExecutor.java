package nu.nerd.beastmaster.commands;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.DoublePredicate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Drop;
import nu.nerd.beastmaster.MobType;

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
        super("beast-mob", "help", "add", "remove", "info", "list",
              "health", "speed", "baby-fraction",
              "add-drop", "remove-drop", "list-drops");
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
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " add <id> <entity-type>");
                    return true;
                }

                String idArg = args[1];
                String entityTypeNameArg = args[2].toUpperCase();

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType != null) {
                    sender.sendMessage(ChatColor.RED + "A mob type named " + idArg + " already exists!");
                    return true;
                }

                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeNameArg);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + entityTypeNameArg + " is not a valid mob type!");
                    return true;
                }

                mobType = new MobType(idArg, entityType);
                BeastMaster.MOBS.addMobType(mobType);
                BeastMaster.MOBS.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
                sender.sendMessage(ChatColor.GOLD + "Added a new mob type: " + mobType.getDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove <id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + idArg + "\"!");
                    return true;
                }

                BeastMaster.MOBS.removeMobType(mobType);
                BeastMaster.MOBS.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
                sender.sendMessage(ChatColor.GOLD + "Removed mob type: " + mobType.getDescription());
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " info <id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + idArg + "\"!");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Mob type: " + mobType.getDescription());
                Collection<Drop> allDrops = mobType.getAllDrops();
                sender.sendMessage(ChatColor.GOLD + (allDrops.isEmpty() ? "No drops defined." : "Drops:"));
                for (Drop drop : allDrops) {
                    sender.sendMessage(drop.toString());
                }
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Too many arguments. Usage: /" + getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Mob types:");
                for (MobType mobType : BeastMaster.MOBS.getMobTypes()) {
                    sender.sendMessage(mobType.getDescription());
                }
                return true;

            } else if (args[0].equals("health")) {
                return handleMobTypeSetProperty(sender, args, "health", "health",
                                                v -> (v > 0), "more than 0",
                                                MobType::setHealth);

            } else if (args[0].equals("speed")) {
                return handleMobTypeSetProperty(sender, args, "speed", "speed",
                                                v -> (v > 0), "more than 0",
                                                MobType::setSpeed);

            } else if (args[0].equals("baby-fraction")) {
                return handleMobTypeSetProperty(sender, args, "baby-fraction", "baby mob fraction",
                                                v -> (v >= 0.0 && v <= 1.0), "in the range [0.0, 1.0]",
                                                MobType::setBabyFraction);

            } else if (args[0].equals("add-drop")) {
                if (args.length < 5 || args.length > 6) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " add-drop <id> <item-id> <chance> <min> [<max>]");
                    return true;
                }

                String idArg = args[1];
                String itemIdArg = args[2];
                String chanceArg = args[3];
                String minArg = args[4];
                String maxArg = (args.length == 6) ? args[5] : null;

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + idArg + "\"!");
                    return true;
                }

                double chance = -1;
                try {
                    chance = Double.parseDouble(chanceArg);
                } catch (NumberFormatException ex) {
                }
                if (chance < 0.0 || chance > 1.0) {
                    sender.sendMessage(ChatColor.RED + "The chance must be a number in the range [0.0, 1.0]!");
                    return true;
                }

                int min = -1;
                try {
                    min = Integer.parseInt(minArg);
                } catch (NumberFormatException ex) {
                }
                if (min < 0) {
                    sender.sendMessage(ChatColor.RED + "The minimum number of drops must be at least 0!");
                    return true;
                }

                int max;
                if (maxArg != null) {
                    max = -1;
                    try {
                        max = Integer.parseInt(maxArg);
                    } catch (NumberFormatException ex) {
                    }
                    if (max < min) {
                        sender.sendMessage(ChatColor.RED + "The maximum number of drops must be at least as many as the minimum number!");
                        return true;
                    }
                } else {
                    max = min;
                }

                Drop oldDrop = mobType.getDrop(itemIdArg);
                Drop newDrop = new Drop(itemIdArg, chance, min, max);
                mobType.addDrop(newDrop);
                BeastMaster.MOBS.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
                if (oldDrop != null) {
                    sender.sendMessage(ChatColor.GOLD + "Replacing " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.GOLD + "Old: " + ChatColor.WHITE + oldDrop);
                    sender.sendMessage(ChatColor.GOLD + "New: " + ChatColor.WHITE + newDrop);
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Adding " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.WHITE + newDrop.toString());
                }
                return true;

            } else if (args[0].equals("remove-drop")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove-drop <id> <item-id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + idArg + "\"!");
                    return true;
                }

                String itemIdArg = args[2];
                Drop drop = mobType.removeDrop(itemIdArg);
                BeastMaster.MOBS.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());

                if (drop == null) {
                    sender.sendMessage(ChatColor.RED + "Mob type " + idArg + " has no drop with ID \"" + itemIdArg + "\"!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Removed " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(drop.toString());
                }
                return true;

            } else if (args[0].equals("list-drops")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Too many arguments. Usage: /" + getName() + " list-drops");
                    return true;
                }

                String id = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(id);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + id + "\"!");
                    return true;
                }

                Collection<Drop> allDrops = mobType.getAllDrops();
                if (allDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Mob type " +
                                       ChatColor.YELLOW + id + ChatColor.GOLD + " has no drops defined.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Drops of mob type " +
                                       ChatColor.YELLOW + id + ChatColor.GOLD + ":");
                }
                for (Drop drop : allDrops) {
                    sender.sendMessage(drop.toString());
                }
                return true;
            }
        }

        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Handle commands that set properties of a {@link MobType}.
     * 
     * @param sender the command sender.
     * @param args the command arguments after /beast-mob.
     * @param expectedCommandArg the subcommand name.
     * @param propertyName the human-readable name of the property for messages.
     * @param valueCheck a predicate that should return true if the value is
     *        valid.
     * @param valueCheckDescription the decription of valid values shown in
     *        error messages.
     * @param setMethod a reference to the MobType instance method used to set
     *        the property.
     * @return the boolean return value of the onCommand() handler.
     */
    protected boolean handleMobTypeSetProperty(CommandSender sender, String[] args,
                                               String expectedCommandArg,
                                               String propertyName,
                                               DoublePredicate valueCheck,
                                               String valueCheckDescription,
                                               BiConsumer<MobType, Double> setMethod) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() +
                               " " + expectedCommandArg + " <id> (<number>|default)");
            return true;
        }

        String id = args[1];
        MobType mobType = BeastMaster.MOBS.getMobType(id);
        if (mobType == null) {
            sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + id + "\"!");
            return true;
        }

        String valueArg = args[2];
        Double value;
        try {
            if (valueArg.equals("default")) {
                value = null;
            } else {
                value = Double.parseDouble(valueArg);
                if (!valueCheck.test(value)) {
                    sender.sendMessage(ChatColor.RED + "The " + propertyName +
                                       " value must be " + valueCheckDescription + ".");
                    return true;
                }
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "The " + propertyName + " value must be a number or \"default\".");
            return true;
        }

        setMethod.accept(mobType, value);
        BeastMaster.MOBS.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
        String formattedValue = (value == null) ? "default" : "" + value;
        sender.sendMessage(ChatColor.GOLD + "The " + propertyName + " of mob type " + ChatColor.YELLOW + mobType.getId() +
                           ChatColor.GOLD + " is now " + ChatColor.YELLOW + formattedValue + ChatColor.GOLD + ".");
        return true;
    }
} // class BeastMobExecutor