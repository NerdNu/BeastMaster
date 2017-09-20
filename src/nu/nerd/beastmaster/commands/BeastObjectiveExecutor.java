package nu.nerd.beastmaster.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.objectives.ObjectiveType;

// --------------------------------------------------------------------------
/**
 * Executor for the /beast-obj command.
 */
public class BeastObjectiveExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastObjectiveExecutor() {
        super("beast-obj", "help", "add", "remove", "list", "info",
              "limit", "range", "height", "time");
        // TODO: command to set loot table.
    }

    // --------------------------------------------------------------------------
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
                    Commands.invalidArguments(sender, getName() + " add <obj-id>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType != null) {
                    Commands.errorNotNull(sender, "objective", idArg);
                    return true;
                }

                objectiveType = new ObjectiveType(idArg);
                BeastMaster.OBJECTIVE_TYPES.addObjectiveType(objectiveType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added a new objective type: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <obj-id>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType == null) {
                    Commands.errorNull(sender, "objective", idArg);
                    return true;
                }

                BeastMaster.OBJECTIVE_TYPES.removeObjectiveType(objectiveType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed objective type: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Objective types:");
                for (ObjectiveType objectiveType : BeastMaster.OBJECTIVE_TYPES.getObjectiveTypes()) {
                    sender.sendMessage(objectiveType.getDescription());
                }
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <obj-id>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType == null) {
                    Commands.errorNull(sender, "objective", idArg);
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Objective: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("limit")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " limit <obj-id> <max>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType == null) {
                    Commands.errorNull(sender, "objective", idArg);
                    return true;
                }

                Integer maxCount = Commands.parseNumber(args[2], Commands::parseInt,
                                                        (x) -> x > 0,
                                                        () -> sender
                                                        .sendMessage(ChatColor.RED + "The maximum number of objectives must be at least one!"),
                                                        null);
                if (maxCount == null) {
                    return true;
                }

                objectiveType.setMaxCount(maxCount);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Objective: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("range")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " range <obj-id> <min> <max>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType == null) {
                    Commands.errorNull(sender, "objective", idArg);
                    return true;
                }

                Integer minRange = Commands.parseNumber(args[2], Commands::parseInt,
                                                        (x) -> x >= 0,
                                                        () -> sender.sendMessage(ChatColor.RED + "The minimum range must be at least zero!"),
                                                        () -> sender.sendMessage(ChatColor.RED + "The minimum range must be an integer!"));
                if (minRange == null) {
                    return true;
                }
                Integer maxRange = Commands.parseNumber(args[3], Commands::parseInt,
                                                        (x) -> x >= minRange,
                                                        () -> sender.sendMessage(ChatColor.RED +
                                                                                 "The maximum range must be at least as big as the minimum range!"),
                                                        () -> sender.sendMessage(ChatColor.RED + "The maximum range must be an integer!"));
                if (maxRange == null) {
                    return true;
                }

                objectiveType.setRange(minRange, maxRange);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Objective: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("height")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " height <obj-id> <min> <max>");
                    return true;
                }

                String idArg = args[1];
                ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(idArg);
                if (objectiveType == null) {
                    Commands.errorNull(sender, "objective", idArg);
                    return true;
                }

                Integer minY = Commands.parseNumber(args[2], Commands::parseInt,
                                                    (x) -> x >= 0,
                                                    () -> sender.sendMessage(ChatColor.RED + "The minimum Y must be at least zero!"),
                                                    () -> sender.sendMessage(ChatColor.RED + "The minimum Y must be an integer!"));
                if (minY == null) {
                    return true;
                }
                Integer maxY = Commands.parseNumber(args[3], Commands::parseInt,
                                                    (y) -> y >= minY && y <= 255,
                                                    () -> sender.sendMessage(ChatColor.RED + "The maximum Y must be between the minimum Y and 255!"),
                                                    () -> sender.sendMessage(ChatColor.RED + "The maximum Y must be an integer!"));
                if (maxY == null) {
                    return true;
                }

                objectiveType.setHeight(minY, maxY);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Objective: " + objectiveType.getDescription());
                return true;

            } else if (args[0].equals("time")) {
                sender.sendMessage(ChatColor.RED + "Not yet implemented; all objectives have unlimited lifetimes!");
                return true;
            }
        }

        return false;
    } // onCommand
} // class BeastObjectiveExecutor