package nu.nerd.beastmaster.commands;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Drop;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.objectives.ObjectiveType;

// ----------------------------------------------------------------------------
/**
 * Executor for the /beast-loot command.
 */
public class BeastLootExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastLootExecutor() {
        super("beast-loot", "help", "add", "remove", "info", "list",
              "add-drop", "remove-drop", "list-drops",
              "objective", "single", "sound", "xp");
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
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " add <loot-id>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet != null) {
                    Commands.errorNotNull(sender, "loot table", idArg);
                    return true;
                }

                dropSet = new DropSet(idArg);
                BeastMaster.LOOTS.addDropSet(dropSet);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added a new loot table: " + dropSet.getDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <loot-id>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                BeastMaster.LOOTS.removeDropSet(dropSet);

                // TODO: remove references to loot table by mobs and mining.

                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed loot table: " + dropSet.getDescription());
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <loot-id>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Loot table: " + dropSet.getDescription());
                Collection<Drop> allDrops = dropSet.getAllDrops();
                sender.sendMessage(ChatColor.GOLD + (allDrops.isEmpty() ? "No drops defined." : "Drops:"));
                for (Drop drop : allDrops) {
                    sender.sendMessage(drop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Loot tables:");
                for (DropSet dropSet : BeastMaster.LOOTS.getDropSets()) {
                    sender.sendMessage(dropSet.getDescription());
                }
                return true;

            } else if (args[0].equals("add-drop")) {
                if (args.length < 5 || args.length > 6) {
                    Commands.invalidArguments(sender, getName() + " add-drop <loot-id> <item-id> <percentage-chance> <min> [<max>]");
                    return true;
                }

                String idArg = args[1];
                String itemIdArg = args[2];
                String chanceArg = args[3];
                String minArg = args[4];
                String maxArg = (args.length == 6) ? args[5] : null;

                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                Double chance = Commands.parseNumber(chanceArg, Commands::parseDouble,
                                                     x -> x >= 0.0 && x <= 100.0,
                                                     () -> sender
                                                     .sendMessage(ChatColor.RED + "The chance must be a percentage in the range 0 through 100!"),
                                                     null);
                if (chance == null) {
                    return true;
                }

                Integer min = Commands.parseNumber(minArg, Commands::parseInt,
                                                   x -> x >= 1,
                                                   () -> sender.sendMessage(ChatColor.RED + "The minimum number of drops must be at least 1!"),
                                                   null);
                if (min == null) {
                    return true;
                }

                Integer max;
                if (maxArg != null) {
                    max = Commands.parseNumber(maxArg, Commands::parseInt,
                                               x -> x >= min,
                                               () -> sender.sendMessage(ChatColor.RED +
                                                                        "The maximum number of drops must be at least as many as the minimum number!"),
                                               null);
                    if (max == null) {
                        return true;
                    }
                } else {
                    max = min;
                }

                Drop oldDrop = dropSet.getDrop(itemIdArg);
                Drop newDrop = new Drop(itemIdArg, chance / 100.0, min, max);
                dropSet.addDrop(newDrop);
                BeastMaster.CONFIG.save();
                if (oldDrop != null) {
                    sender.sendMessage(ChatColor.GOLD + "Replacing " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.GOLD + "Old: " + ChatColor.WHITE + oldDrop.getLongDescription());
                    sender.sendMessage(ChatColor.GOLD + "New: " + ChatColor.WHITE + newDrop.getLongDescription());
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Adding " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.WHITE + newDrop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("remove-drop")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " remove-drop <loot-id> <item-id>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                String itemIdArg = args[2];
                Drop drop = dropSet.removeDrop(itemIdArg);
                BeastMaster.CONFIG.save();

                if (drop == null) {
                    sender.sendMessage(ChatColor.RED + "Loot table " + idArg + " has no drop with ID \"" + itemIdArg + "\"!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Removed " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(drop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("list-drops")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " list-drops <loot-id>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                Collection<Drop> allDrops = dropSet.getAllDrops();
                if (allDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Loot table " +
                                       ChatColor.YELLOW + idArg + ChatColor.GOLD + " has no drops defined.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Drops of loot table " +
                                       ChatColor.YELLOW + idArg + ChatColor.GOLD + ":");
                }
                for (Drop drop : allDrops) {
                    sender.sendMessage(drop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("single")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " single <loot-id> <yes-or-no>");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                String yesNoArg = args[2];
                Boolean single = Commands.parseBoolean(sender, yesNoArg);
                if (single == null) {
                    return true;
                }

                dropSet.setSingle(single);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Loot table " + ChatColor.YELLOW + idArg +
                                   ChatColor.GOLD + " is now configured for " +
                                   ChatColor.YELLOW + (single ? "single" : "multiple") +
                                   ChatColor.GOLD + " drop operation.");
                return true;

            } else if (args[0].equals("objective")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " objective <loot-id> <item-id> (<obj-id>|none)");
                    return true;
                }

                String idArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", idArg);
                    return true;
                }

                String itemIdArg = args[2];
                Drop drop = dropSet.getDrop(itemIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + idArg, itemIdArg);
                    return true;
                }

                String objIdArg = args[3];
                if (objIdArg.equals("none")) {
                    drop.setObjectiveType(null);
                    sender.sendMessage(ChatColor.GOLD + "Cleared objective on drop " + drop.getLongDescription());
                } else {
                    ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(objIdArg);
                    if (objectiveType == null) {
                        Commands.errorNull(sender, "objective type", objIdArg);
                        return true;
                    }

                    drop.setObjectiveType(objIdArg);
                    sender.sendMessage(ChatColor.GOLD + "Set objective on drop " + drop.getLongDescription());
                }
                BeastMaster.CONFIG.save();
                return true;
            }
        }

        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Handle commands that set properties of a {@link DropSet}.
     * 
     * @param sender the command sender.
     * @param args the command arguments after /beast-loot.
     * @param expectedCommandArg the subcommand name.
     * @param propertyName the human-readable name of the property for messages.
     * @param inRange a predicate that should return true if the value is valid.
     * @param valueCheckDescription the description of valid values shown in
     *        error messages.
     * @param setMethod a reference to the DropSet instance method used to set
     *        the property.
     * @return the boolean return value of the onCommand() handler.
     */
    protected boolean handleDropSetSetProperty(CommandSender sender, String[] args,
                                               String expectedCommandArg,
                                               String propertyName,
                                               Predicate<Double> inRange,
                                               String valueCheckDescription,
                                               BiConsumer<DropSet, Double> setMethod) {
        if (args.length != 3) {
            Commands.invalidArguments(sender, getName() + " " + expectedCommandArg +
                                              " <loot-id> (<number>|default)");
            return true;
        }

        String idArg = args[1];
        DropSet dropSet = BeastMaster.LOOTS.getDropSet(idArg);
        if (dropSet == null) {
            Commands.errorNull(sender, "loot table", idArg);
            return true;
        }

        Runnable rangeError = () -> sender.sendMessage(ChatColor.RED + "The " + propertyName + " value must be " + valueCheckDescription + ".");
        Runnable formatError = () -> sender.sendMessage(ChatColor.RED + "The " + propertyName + " value must be a number or \"default\".");
        Optional<Double> optionalValue = Commands.parseNumberDefaulted(args[2], "default",
                                                                       Commands::parseDouble,
                                                                       inRange,
                                                                       rangeError, formatError);

        if (optionalValue != null) {
            Double value = optionalValue.orElse(null);
            setMethod.accept(dropSet, value);
            BeastMaster.CONFIG.save();
            String formattedValue = (value == null) ? "default" : "" + value;
            sender.sendMessage(ChatColor.GOLD + "The " + propertyName + " of loot table " +
                               ChatColor.YELLOW + dropSet.getId() + ChatColor.GOLD + " is now " +
                               ChatColor.YELLOW + formattedValue + ChatColor.GOLD + ".");
        }
        return true;
    }
} // class BeastLootExecutor