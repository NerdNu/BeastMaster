package nu.nerd.beastmaster.commands;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.PotionSet;
import nu.nerd.beastmaster.ProbablePotion;

// --------------------------------------------------------------------------
/**
 * Executor for the {@code /beast-potion} command.
 */
public class BeastPotionExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastPotionExecutor() {
        super("beast-potion", "help", "add", "remove", "list",
              "add-potion", "remove-potion", "list-potions");
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

        if (args[0].equals("add")) {
            if (args.length != 2) {
                Commands.invalidArguments(sender, getName() + " add <potion-set-id>");
                return true;
            }

            String idArg = args[1];
            PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(idArg);
            if (potionSet != null) {
                Commands.errorNotNull(sender, "potion set", idArg);
                return true;
            }

            potionSet = new PotionSet(idArg);
            BeastMaster.POTIONS.addPotionSet(potionSet);
            BeastMaster.CONFIG.save();
            sender.sendMessage(ChatColor.GOLD + "Added a new potion set: " + ChatColor.YELLOW + potionSet.getId());
            return true;

        } else if (args[0].equals("remove")) {
            if (args.length != 2) {
                Commands.invalidArguments(sender, getName() + " remove <potion-set-id>");
                return true;
            }

            String idArg = args[1];
            PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(idArg);
            if (potionSet == null) {
                Commands.errorNull(sender, "potion set", idArg);
                return true;
            }

            BeastMaster.POTIONS.removePotionSet(potionSet);

            BeastMaster.CONFIG.save();
            sender.sendMessage(ChatColor.GOLD + "Removed potion set: " + potionSet.getDescription());
            return true;

        } else if (args[0].equals("list")) {
            if (args.length != 1) {
                Commands.invalidArguments(sender, getName() + " list");
                return true;
            }

            Collection<PotionSet> potionSets = BeastMaster.POTIONS.getSortedPotionSets();
            if (potionSets.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + "There are no potion sets.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Potion sets: " +
                                   potionSets.stream().map(s -> ChatColor.YELLOW + s.getId())
                                   .collect(Collectors.joining(ChatColor.WHITE + ", ")));
            }
            return true;

        } else if (args[0].equals("add-potion")) {
            if (args.length < 6 || args.length > 9) {
                Commands.invalidArguments(sender, getName() + " add-potion <potion-set-id> <percentage-chance> " +
                                                  "<seconds> <potion-type> <amplifier> [<particles>] [<ambient>] [<icon>]");
                listPotionEffectTypes(sender);
                return true;
            }

            String potionSetIdArg = args[1];
            String chanceArg = args[2];
            String secondsArg = args[3];
            String potionTypeArg = args[4];
            String amplifierArg = args[5];
            String particlesArg = args.length >= 7 ? args[6] : "false";
            String ambientArg = args.length >= 8 ? args[7] : "false";
            String iconArg = args.length == 9 ? args[8] : "false";

            PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(potionSetIdArg);
            if (potionSet == null) {
                Commands.errorNull(sender, "potion set", potionSetIdArg);
                return true;
            }

            Runnable chanceRangeError = () -> sender.sendMessage(ChatColor.RED + "The chance must be a percentage in the range 0 through 100!");
            Double chance = Commands.parseNumber(chanceArg, Commands::parseDouble,
                                                 x -> x >= 0.0 && x <= 100.0,
                                                 chanceRangeError, null);
            if (chance == null) {
                return true;
            }

            Runnable secondsRangeError = () -> sender.sendMessage(ChatColor.RED + "The <seconds> value must be positive or \"inf\" for infinite.");
            Optional<Double> seconds = Commands.parseNumberDefaulted(secondsArg, "inf",
                                                                     Commands::parseDouble,
                                                                     x -> (x > 0.0),
                                                                     secondsRangeError, null);
            if (seconds == null) {
                return true;
            }
            int ticks = seconds.isPresent() ? (int) Math.round(20 * seconds.get()) : Integer.MAX_VALUE;

            PotionEffectType potionType = PotionEffectType.getByName(potionTypeArg);
            if (potionType == null) {
                sender.sendMessage(ChatColor.RED + "Invalid potion type: " + potionTypeArg);
                listPotionEffectTypes(sender);
                return true;
            }

            Integer amplifier = Commands.parseNumber(amplifierArg, Commands::parseInt,
                                                     x -> x >= 1,
                                                     () -> sender.sendMessage(ChatColor.RED + "The amplifier must be 1 or greater!"),
                                                     null);
            if (amplifier == null) {
                return true;
            }

            Boolean particles = Commands.parseBoolean(sender, particlesArg, "particles");
            if (particles == null) {
                return true;
            }

            Boolean ambient = Commands.parseBoolean(sender, ambientArg, "ambient");
            if (ambient == null) {
                return true;
            }

            Boolean icon = Commands.parseBoolean(sender, iconArg, "icon");
            if (icon == null) {
                return true;
            }

            ProbablePotion oldPotion = potionSet.getPotion(potionType);
            PotionEffect newEffect = new PotionEffect(potionType, ticks, amplifier - 1, ambient, particles, icon);
            ProbablePotion newPotion = new ProbablePotion(newEffect, chance / 100.0);
            potionSet.addPotion(newPotion);
            BeastMaster.CONFIG.save();

            if (oldPotion != null) {
                sender.sendMessage(ChatColor.GOLD + "Replacing potion " + ChatColor.YELLOW + potionSetIdArg +
                                   ChatColor.GOLD + ":");
                sender.sendMessage(ChatColor.GOLD + "Old: " + ChatColor.WHITE + oldPotion.getDescription());
                sender.sendMessage(ChatColor.GOLD + "New: " + ChatColor.WHITE + newPotion.getDescription());
            } else {
                sender.sendMessage(ChatColor.GOLD + "Adding potion " + ChatColor.YELLOW + potionSetIdArg +
                                   ChatColor.GOLD + ":");
                sender.sendMessage(ChatColor.WHITE + newPotion.getDescription());
            }
            return true;

        } else if (args[0].equals("remove-potion")) {
            if (args.length != 3) {
                Commands.invalidArguments(sender, getName() + " remove-potion <potion-set-id> <potion-type>");
                return true;
            }

            String potionSetIdArg = args[1];
            PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(potionSetIdArg);
            if (potionSet == null) {
                Commands.errorNull(sender, "potion set", potionSetIdArg);
                return true;
            }

            String potionTypeArg = args[2];
            PotionEffectType potionType = PotionEffectType.getByName(potionTypeArg);
            if (potionType == null) {
                sender.sendMessage(ChatColor.RED + potionTypeArg + " is not a valid potion type.");
                sender.sendMessage(ChatColor.RED + "This potion set contains: " +
                                   potionSet.getSortedPotionTypes().stream().map(p -> p.getName()).collect(Collectors.joining(", ")));
                return true;
            }

            ProbablePotion potion = potionSet.removePotion(potionType);
            if (potion == null) {
                sender.sendMessage(ChatColor.RED + "Potion set " + potionSetIdArg + " has no potion with type \"" + potionTypeArg + "\"!");
            } else {
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed potion " + ChatColor.YELLOW + potionSetIdArg +
                                   ChatColor.GOLD + ":");
                sender.sendMessage(potion.getDescription());
            }
            return true;

        } else if (args[0].equals("list-potions")) {
            if (args.length != 2) {
                Commands.invalidArguments(sender, getName() + " list-potions <potion-set-id>");
                return true;
            }

            String idArg = args[1];
            PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(idArg);
            if (potionSet == null) {
                Commands.errorNull(sender, "potion set", idArg);
                return true;
            }

            if (potionSet.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + "Potion set " +
                                   ChatColor.YELLOW + potionSet.getId() +
                                   ChatColor.GOLD + " contains no potions.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Potion set: " + potionSet.getDescription());
            }
            return true;

        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * List all potion types to the sender.
     * 
     * @param sender the command sender.
     */
    protected void listPotionEffectTypes(CommandSender sender) {
        String effects = Stream.of(PotionEffectType.values())
        .map(e -> ChatColor.YELLOW + e.getName().toLowerCase())
        .collect(Collectors.joining(ChatColor.WHITE + ", "));
        sender.sendMessage(ChatColor.GOLD + "Potion types: " + effects);
    }
} // class BeastPotionExecutor