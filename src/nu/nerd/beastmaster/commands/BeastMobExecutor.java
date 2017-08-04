package nu.nerd.beastmaster.commands;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Drop;
import nu.nerd.beastmaster.MobType;
import nu.nerd.beastmaster.objectives.ObjectiveType;
import nu.nerd.beastmaster.zones.Zone;

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
              "add-drop", "remove-drop", "objective", "list-drops");
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
                    Commands.invalidArguments(sender, getName() + " add <mob-id> <entity-type>");
                    return true;
                }

                String idArg = args[1];
                String entityTypeNameArg = args[2].toUpperCase();

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType != null) {
                    Commands.errorNotNull(sender, "mob type", idArg);
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
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added a new mob type: " + mobType.getDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <mob-id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                BeastMaster.MOBS.removeMobType(mobType);

                // Also remove the mob type from spawns in all known zones.
                for (Zone zone : BeastMaster.ZONES.getZones()) {
                    zone.removeSpawn(mobType.getId());
                }

                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed mob type: " + mobType.getDescription());
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <mob-id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Mob type: " + mobType.getDescription());
                Collection<Drop> allDrops = mobType.getDropSet().getAllDrops();
                sender.sendMessage(ChatColor.GOLD + (allDrops.isEmpty() ? "No drops defined." : "Drops:"));
                for (Drop drop : allDrops) {
                    sender.sendMessage(drop.toString());
                }
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
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
                    Commands.invalidArguments(sender, getName() + " add-drop <mob-id> <item-id> <chance> <min> [<max>]");
                    return true;
                }

                String idArg = args[1];
                String itemIdArg = args[2];
                String chanceArg = args[3];
                String minArg = args[4];
                String maxArg = (args.length == 6) ? args[5] : null;

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                Double chance = Commands.parseNumber(chanceArg, Commands::parseDouble,
                                                     x -> x >= 0.0 && x <= 1.0,
                                                     () -> sender.sendMessage(ChatColor.RED + "The chance must be a number in the range [0.0, 1.0]!"),
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

                Drop oldDrop = mobType.getDropSet().getDrop(itemIdArg);
                Drop newDrop = new Drop(itemIdArg, chance, min, max);
                mobType.getDropSet().addDrop(newDrop);
                BeastMaster.CONFIG.save();
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
                    Commands.invalidArguments(sender, getName() + " remove-drop <mob-id> <item-id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                String itemIdArg = args[2];
                Drop drop = mobType.getDropSet().removeDrop(itemIdArg);
                BeastMaster.CONFIG.save();

                if (drop == null) {
                    sender.sendMessage(ChatColor.RED + "Mob type " + idArg + " has no drop with ID \"" + itemIdArg + "\"!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Removed " + ChatColor.YELLOW + idArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(drop.toString());
                }
                return true;

            } else if (args[0].equals("objective")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " objective <mob-id> <item-id> (<obj-id>|none)");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                String itemIdArg = args[2];
                Drop drop = mobType.getDropSet().getDrop(itemIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + idArg, itemIdArg);
                    return true;
                }

                String objIdArg = args[3];
                if (objIdArg.equals("none")) {
                    drop.setObjectiveType(null);
                    sender.sendMessage(ChatColor.GOLD + "Cleared objective on drop " + drop.toString());
                } else {
                    ObjectiveType objectiveType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(objIdArg);
                    if (objectiveType == null) {
                        Commands.errorNull(sender, "objective type", objIdArg);
                        return true;
                    }

                    drop.setObjectiveType(objIdArg);
                    sender.sendMessage(ChatColor.GOLD + "Set objective on drop " + drop.toString());
                }
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("list-drops")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " list-drops <mob-id>");
                    return true;
                }

                String idArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", idArg);
                    return true;
                }

                Collection<Drop> allDrops = mobType.getDropSet().getAllDrops();
                if (allDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Mob type " +
                                       ChatColor.YELLOW + idArg + ChatColor.GOLD + " has no drops defined.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Drops of mob type " +
                                       ChatColor.YELLOW + idArg + ChatColor.GOLD + ":");
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
     * @param inRange a predicate that should return true if the value is valid.
     * @param valueCheckDescription the decription of valid values shown in
     *        error messages.
     * @param setMethod a reference to the MobType instance method used to set
     *        the property.
     * @return the boolean return value of the onCommand() handler.
     */
    protected boolean handleMobTypeSetProperty(CommandSender sender, String[] args,
                                               String expectedCommandArg,
                                               String propertyName,
                                               Predicate<Double> inRange,
                                               String valueCheckDescription,
                                               BiConsumer<MobType, Double> setMethod) {
        if (args.length != 3) {
            Commands.invalidArguments(sender, getName() + " " + expectedCommandArg +
                                              " <mob-id> (<number>|default)");
            return true;
        }

        String idArg = args[1];
        MobType mobType = BeastMaster.MOBS.getMobType(idArg);
        if (mobType == null) {
            Commands.errorNull(sender, "mob type", idArg);
            return true;
        }

        Runnable rangeError = () -> sender.sendMessage(ChatColor.RED + "The " + propertyName +
                                                       " value must be " + valueCheckDescription + ".");
        Runnable formatError = () -> sender.sendMessage(ChatColor.RED + "The " + propertyName +
                                                        " value must be a number or \"default\".");
        Optional<Double> optionalValue = Commands.parseNumberDefaulted(args[2], "default",
                                                                       Commands::parseDouble,
                                                                       inRange,
                                                                       rangeError, formatError);

        if (optionalValue != null) {
            Double value = optionalValue.orElse(null);
            setMethod.accept(mobType, value);
            BeastMaster.CONFIG.save();
            String formattedValue = (value == null) ? "default" : "" + value;
            sender.sendMessage(ChatColor.GOLD + "The " + propertyName + " of mob type " +
                               ChatColor.YELLOW + mobType.getId() + ChatColor.GOLD + " is now " +
                               ChatColor.YELLOW + formattedValue + ChatColor.GOLD + ".");
        }
        return true;
    }
} // class BeastMobExecutor