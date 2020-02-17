package nu.nerd.beastmaster.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Drop;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.DropType;
import nu.nerd.beastmaster.Util;
import nu.nerd.beastmaster.objectives.ObjectiveType;

// ----------------------------------------------------------------------------
/**
 * Executor for the {@code /beast-loot} command.
 */
public class BeastLootExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastLootExecutor() {
        super("beast-loot", "help", "add", "remove", "info", "list",
              "add-drop", "remove-drop", "list-drops",
              "single", "objective", "logged", "sound", "xp",
              "invulnerable", "glowing", "direct");
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

                sender.sendMessage(ChatColor.GOLD + "Loot table: " + dropSet.getShortDescription());
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
                ArrayList<DropSet> sortedDropSets = new ArrayList<>(BeastMaster.LOOTS.getDropSets());
                sortedDropSets.sort((s1, s2) -> s1.getId().compareTo(s2.getId()));
                sender.sendMessage(sortedDropSets.stream()
                .map((dropSet) -> ChatColor.YELLOW + dropSet.getId())
                .collect(Collectors.joining(ChatColor.WHITE + ", ")));
                return true;

            } else if (args[0].equals("add-drop")) {
                if (args.length < 4 || args.length > 7) {
                    Commands.invalidArguments(sender, getName() + " add-drop <loot-id> <drop-type> [<id>] <percentage-chance> [<min>] [<max>]");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropTypeArg = args[2];
                if (!DropType.isDropType(dropTypeArg)) {
                    sender.sendMessage(ChatColor.RED + dropTypeArg + " is not a valid drop type.");
                    sender.sendMessage(ChatColor.GOLD + "Drop types: " +
                                       Util.alternateColours(Stream.of(DropType.values()).map(t -> t.toString().toLowerCase())
                                       .collect(Collectors.toList()), ChatColor.GRAY + " ", ChatColor.WHITE, ChatColor.YELLOW));
                    return true;
                }

                DropType dropType = DropType.valueOf(dropTypeArg.toUpperCase());
                int chanceIndex = dropType.usesId() ? 4 : 3;
                String dropIdArg = dropType.usesId() ? args[3] : dropType.name();
                if (chanceIndex >= args.length) {
                    Commands.invalidArguments(sender, getName() + " add-drop <loot-id> <drop-type> [<id>] <percentage-chance> [<min>] [<max>]");
                    return true;
                }

                String chanceArg = args[chanceIndex];
                String minArg = "1";
                if (args.length > chanceIndex + 1) {
                    if (dropType.usesId()) {
                        minArg = args[chanceIndex + 1];
                    } else {
                        sender.sendMessage(ChatColor.RED + "Min and max arguments are ignored for " + dropType + " drops.");
                    }
                }
                String maxArg = (args.length > chanceIndex + 2 && dropType.usesId()) ? args[chanceIndex + 2] : minArg;

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

                Integer max = Commands.parseNumber(maxArg, Commands::parseInt,
                                                   x -> x >= min,
                                                   () -> sender.sendMessage(ChatColor.RED +
                                                                            "The maximum number of drops must be at least as many as the minimum number!"),
                                                   null);
                if (max == null) {
                    return true;
                }

                Drop oldDrop = dropSet.getDrop(dropIdArg);
                Drop newDrop;
                if (oldDrop == null) {
                    newDrop = new Drop(dropType, dropIdArg, chance / 100.0, min, max);
                } else {
                    // Clone all the other fields not specified by this command.
                    newDrop = oldDrop.clone();
                    newDrop.setDropChance(chance / 100.0);
                    newDrop.setMinAmount(min);
                    newDrop.setMaxAmount(max);
                }
                dropSet.addDrop(newDrop);
                BeastMaster.CONFIG.save();

                if (oldDrop != null) {
                    sender.sendMessage(ChatColor.GOLD + "Replacing " + ChatColor.YELLOW + lootIdArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.GOLD + "Old: " + ChatColor.WHITE + oldDrop.getLongDescription());
                    sender.sendMessage(ChatColor.GOLD + "New: " + ChatColor.WHITE + newDrop.getLongDescription());
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Adding " + ChatColor.YELLOW + lootIdArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(ChatColor.WHITE + newDrop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("remove-drop")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " remove-drop <loot-id> <id>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.removeDrop(dropIdArg);
                BeastMaster.CONFIG.save();

                if (drop == null) {
                    sender.sendMessage(ChatColor.RED + "Loot table " + lootIdArg + " has no drop with ID \"" + dropIdArg + "\"!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Removed " + ChatColor.YELLOW + lootIdArg +
                                       ChatColor.GOLD + " drop:");
                    sender.sendMessage(drop.getLongDescription());
                }
                return true;

            } else if (args[0].equals("list-drops")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " list-drops <loot-id>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                Collection<Drop> allDrops = dropSet.getAllDrops();
                if (allDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Loot table " +
                                       ChatColor.YELLOW + lootIdArg + ChatColor.GOLD + " has no drops defined.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Drops of loot table " +
                                       ChatColor.YELLOW + lootIdArg + ChatColor.GOLD + ":");
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

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String yesNoArg = args[2];
                Boolean single = Commands.parseBoolean(sender, yesNoArg, "single");
                if (single == null) {
                    return true;
                }

                dropSet.setSingle(single);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Loot table " + ChatColor.YELLOW + lootIdArg +
                                   ChatColor.GOLD + " is now configured for " +
                                   ChatColor.YELLOW + (single ? "single" : "multiple") +
                                   ChatColor.GOLD + " drop operation.");
                return true;

            } else if (args[0].equals("objective")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " objective <loot-id> <item-id> (<obj-id>|none)");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                if (drop.getDropType() != DropType.ITEM) {
                    sender.sendMessage(ChatColor.RED + "Only ITEM drops can have an associated objective.");
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

            } else if (args[0].equals("logged")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " logged <loot-id> <id> <yes-or-no>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                String yesNoArg = args[3];
                Boolean logged = Commands.parseBoolean(sender, yesNoArg, "logged");
                if (logged == null) {
                    return true;
                }

                drop.setLogged(logged);
                String change = logged ? "Enabled" : "Disabled";
                sender.sendMessage(ChatColor.GOLD + change + " logging of " + drop.getLongDescription());
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("sound")) {
                if (args.length != 4 && args.length != 6) {
                    Commands.invalidArguments(sender, getName() + " sound <loot-id> <id> <sound> [<range> <pitch>]");
                    sender.sendMessage(ChatColor.GOLD + "Sound names are case insensitive:");
                    sender.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE +
                                       "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                String soundArg = args[3];
                Sound sound;
                try {
                    sound = (soundArg.equalsIgnoreCase("none") ? null : Sound.valueOf(soundArg.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + soundArg + " is not a valid sound.");
                    return true;
                }

                Float range = 15.0f;
                Float pitch = 1.0f;
                if (args.length == 6) {
                    String rangeArg = args[4];
                    range = Commands.parseNumber(rangeArg, Commands::parseFloat,
                                                 r -> (r > 0.0f),
                                                 () -> sender.sendMessage(ChatColor.RED + "The range must be a number greater than 0."),
                                                 null);
                    if (range == null) {
                        return true;
                    }

                    String pitchArg = args[5];
                    pitch = Commands.parseNumber(pitchArg, Commands::parseFloat,
                                                 p -> (p >= 0.5f && p <= 2.0f),
                                                 () -> sender.sendMessage(ChatColor.RED + "The pitch must be in the range 0.5 to 2.0."),
                                                 null);
                    if (pitch == null) {
                        return true;
                    }
                }

                drop.setSound(sound);
                drop.setSoundVolume(range / 15);
                drop.setSoundPitch(pitch);
                sender.sendMessage(ChatColor.GOLD + "Changed the drop sound of " +
                                   ChatColor.YELLOW + drop.getId() +
                                   ChatColor.GOLD + " in table " +
                                   ChatColor.YELLOW + lootIdArg +
                                   ChatColor.GOLD + " to " + drop.getSoundDescription() +
                                   ChatColor.GOLD + ".");
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("xp")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " xp <loot-id> <id> <xp>");
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

                int oldXp = drop.getExperience();
                String xpArg = args[3];
                Integer newXp = Commands.parseNumber(xpArg, Commands::parseInt,
                                                     (x) -> (x >= 0),
                                                     () -> sender
                                                     .sendMessage(ChatColor.RED + "The amount of experience must be a non-negative integer."),
                                                     null);
                if (newXp == null) {
                    return true;
                }

                drop.setExperience(newXp);
                sender.sendMessage(ChatColor.GOLD + "Changed the dropped XP of " +
                                   ChatColor.YELLOW + drop.getId() +
                                   ChatColor.GOLD + " in table " +
                                   ChatColor.YELLOW + idArg +
                                   ChatColor.GOLD + " from " + ChatColor.YELLOW + oldXp +
                                   ChatColor.GOLD + " to " + ChatColor.YELLOW + newXp +
                                   ChatColor.GOLD + ".");
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("invulnerable")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " invulnerable <loot-id> <id> <yes-or-no>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                String yesNoArg = args[3];
                Boolean flag = Commands.parseBoolean(sender, yesNoArg, "invulnerable");
                if (flag == null) {
                    return true;
                }

                drop.setInvulnerable(flag);
                String change = flag ? "Enabled" : "Disabled";
                sender.sendMessage(ChatColor.GOLD + change + " invulnerability of " + drop.getLongDescription());
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("glowing")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " glowing <loot-id> <id> <yes-or-no>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                String yesNoArg = args[3];
                Boolean flag = Commands.parseBoolean(sender, yesNoArg, "glowing");
                if (flag == null) {
                    return true;
                }

                drop.setGlowing(flag);
                String change = flag ? "Enabled" : "Disabled";
                sender.sendMessage(ChatColor.GOLD + change + " glow effect of " + drop.getLongDescription());
                BeastMaster.CONFIG.save();
                return true;
            } else if (args[0].equals("direct")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " direct <loot-id> <item-id> <yes-or-no>");
                    return true;
                }

                String lootIdArg = args[1];
                DropSet dropSet = BeastMaster.LOOTS.getDropSet(lootIdArg);
                if (dropSet == null) {
                    Commands.errorNull(sender, "loot table", lootIdArg);
                    return true;
                }

                String dropIdArg = args[2];
                Drop drop = dropSet.getDrop(dropIdArg);
                if (drop == null) {
                    Commands.errorNull(sender, "drop of " + lootIdArg, dropIdArg);
                    return true;
                }

                String yesNoArg = args[3];
                Boolean flag = Commands.parseBoolean(sender, yesNoArg, "direct");
                if (flag == null) {
                    return true;
                }

                drop.setDirect(flag);
                String change = flag ? "Enabled" : "Disabled";
                sender.sendMessage(ChatColor.GOLD + change + " direct drop flag of " + drop.getLongDescription());
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