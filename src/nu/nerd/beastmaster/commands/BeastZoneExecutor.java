package nu.nerd.beastmaster.commands;

import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.MobType;
import nu.nerd.beastmaster.zones.Zone;

// ----------------------------------------------------------------------------
/**
 * Executor for the /beast-zone command.
 */
public class BeastZoneExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastZoneExecutor() {
        super("beast-zone", "help", "add", "remove", "list",
              "add-spawn", "remove-spawn", "list-spawns");
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
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " add <zone-id> <world>");
                    return true;
                }

                String zoneArg = args[1];
                String worldArg = args[2];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone != null) {
                    sender.sendMessage(ChatColor.RED + "A zone named " + zoneArg + " already exists!");
                    return true;
                }
                World world = Bukkit.getWorld(worldArg);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "There is no world named " + worldArg + "!");
                    return true;
                }

                zone = new Zone(zoneArg, world);
                BeastMaster.ZONES.addZone(zone);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added new zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + " in world " +
                                   ChatColor.YELLOW + zone.getWorld().getName() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    sender.sendMessage(ChatColor.RED + "There is no zone named \"" + zoneArg + "\".");
                    return true;
                }

                BeastMaster.ZONES.removeZone(zone);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Zones:");
                for (Zone zone : BeastMaster.ZONES.getZones()) {
                    sender.sendMessage(zone.getDescription());
                }
                return true;

            } else if (args[0].equals("add-spawn")) {
                if (args.length != 4) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " add-spawn <zone-id> <mob-id> <weight>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    sender.sendMessage(ChatColor.RED + "There is no zone named \"" + zoneArg + "\".");
                    return true;
                }

                String mobIdArg = args[2];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + mobIdArg + "\"!");
                    return true;
                }

                String weightArg = args[3];
                double weight = -1;
                try {
                    weight = Double.parseDouble(weightArg);
                } catch (NumberFormatException ex) {
                }
                if (weight <= 0) {
                    sender.sendMessage(ChatColor.RED + "The weight must be a positive number!");
                    return true;
                }

                zone.addSpawn(mobIdArg, weight);
                BeastMaster.CONFIG.save();
                double percent = 100 * weight / zone.getTotalWeight();
                sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                   ChatColor.GOLD + " will spawn mob " + ChatColor.YELLOW + mobIdArg +
                                   ChatColor.GOLD + " with weight " + ChatColor.YELLOW + weight +
                                   ChatColor.GOLD + " (" + String.format("%4.2f", percent) + "%).");
                return true;

            } else if (args[0].equals("remove-spawn")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove-spawn <zone-id> <mob-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    sender.sendMessage(ChatColor.RED + "There is no zone named \"" + zoneArg + "\".");
                    return true;
                }

                String mobIdArg = args[2];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    sender.sendMessage(ChatColor.RED + "There is no mob type named \"" + mobIdArg + "\"!");
                    return true;
                }

                String removedMobId = zone.removeSpawn(mobIdArg);
                if (removedMobId == null) {
                    sender.sendMessage(ChatColor.GOLD + "Mob type " + ChatColor.YELLOW + mobIdArg +
                                       ChatColor.GOLD + " already doesn't spawn in zone " +
                                       ChatColor.YELLOW + zoneArg + ChatColor.GOLD + ".");
                } else {
                    BeastMaster.CONFIG.save();
                    sender.sendMessage(ChatColor.GOLD + "Mob type " + ChatColor.YELLOW + mobIdArg +
                                       ChatColor.GOLD + " will no longer spawn in zone " +
                                       ChatColor.YELLOW + zoneArg + ChatColor.GOLD + ".");
                }
                return true;

            } else if (args[0].equals("list-spawns")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " list-spawns <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    sender.sendMessage(ChatColor.RED + "There is no zone named \"" + zoneArg + "\".");
                    return true;
                }

                Map<String, Double> spawns = zone.getSpawnWeights();
                if (spawns.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " has no configured spawns.");
                } else {
                    StringBuilder s = new StringBuilder();
                    s.append(ChatColor.GOLD).append("Spawns in zone ");
                    s.append(ChatColor.YELLOW).append(zoneArg);
                    s.append(ChatColor.GOLD).append(": ").append(ChatColor.WHITE);
                    s.append(spawns.entrySet().stream()
                    .map(e -> String.format("%4.2f", 100 * e.getValue() / zone.getTotalWeight()) + "% " +
                              ChatColor.YELLOW + e.getKey() + ChatColor.WHITE)
                    .collect(Collectors.joining(", ")));
                    sender.sendMessage(s.toString());
                }
                return true;
            }
        }

        return false;
    } // onCommand
} // class BeastZoneExecutor