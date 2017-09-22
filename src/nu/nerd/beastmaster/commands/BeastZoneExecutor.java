package nu.nerd.beastmaster.commands;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;
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
        super("beast-zone", "help", "add", "remove", "square", "list",
              "add-spawn", "remove-spawn", "list-spawns",
              "add-block", "remove-block", "list-blocks");
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
                    Commands.invalidArguments(sender, getName() + " add <zone-id> <world>");
                    return true;
                }

                String zoneArg = args[1];
                String worldArg = args[2];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone != null) {
                    Commands.errorNotNull(sender, "zone", zoneArg);
                    return true;
                }
                World world = Bukkit.getWorld(worldArg);
                if (world == null) {
                    Commands.errorNull(sender, "world", worldArg);
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
                    Commands.invalidArguments(sender, getName() + " remove <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                BeastMaster.ZONES.removeZone(zone);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("square")) {
                if (args.length != 5) {
                    Commands.invalidArguments(sender, getName() + " square <zone-id> <x> <z> <radius>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                Integer centreX = Commands.parseNumber(args[2], Commands::parseInt,
                                                       n -> true,
                                                       () -> sender.sendMessage(ChatColor.RED + "The X coordinate must be an integer!"),
                                                       null);
                if (centreX == null) {
                    return true;
                }

                Integer centreZ = Commands.parseNumber(args[3], Commands::parseInt,
                                                       n -> true,
                                                       () -> sender.sendMessage(ChatColor.RED + "The Z coordinate must be an integer!"),
                                                       null);
                if (centreZ == null) {
                    return true;
                }

                Integer radius = Commands.parseNumber(args[4], Commands::parseInt,
                                                      n -> n >= 0,
                                                      () -> sender.sendMessage(ChatColor.RED + "The radius must be at least zero!"),
                                                      () -> sender.sendMessage(ChatColor.RED + "The radius must be a non-negative integer!"));
                if (radius == null) {
                    return true;
                }

                zone.setSquareBounds(centreX, centreZ, radius);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Set bounds for " + zone.getDescription() + ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Zones:");
                for (Zone zone : BeastMaster.ZONES.getZones()) {
                    sender.sendMessage(zone.getDescription());
                }
                return true;

            } else if (args[0].equals("add-spawn")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " add-spawn <zone-id> <mob-id> <weight>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                String mobIdArg = args[2];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
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
                    Commands.invalidArguments(sender, getName() + " remove-spawn <zone-id> <mob-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                String mobIdArg = args[2];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
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
                    Commands.invalidArguments(sender, getName() + " list-spawns <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
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
            } else if (args[0].equals("add-block")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " add-block <zone-id> <material> <loot-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                Material material = Commands.parseMaterial(sender, args[2]);
                if (material == null) {
                    return true;
                }

                // Allow the loot table for a material to be set even if the
                // table is not yet defined.
                String lootArg = args[3];
                zone.setMiningDropsId(material, lootArg);
                BeastMaster.CONFIG.save();

                DropSet drops = BeastMaster.LOOTS.getDropSet(lootArg);
                String dropsDescription = (drops != null) ? drops.getDescription()
                                                          : ChatColor.RED + lootArg + ChatColor.GOLD + " (not defined)";
                sender.sendMessage(ChatColor.GOLD + "When " + ChatColor.YELLOW + material +
                                   ChatColor.GOLD + " is broken in zone " + ChatColor.YELLOW + zoneArg +
                                   ChatColor.GOLD + " loot will drop from table " + dropsDescription +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("remove-block")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " remove-block <zone-id> <material>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                Material material = Commands.parseMaterial(sender, args[2]);
                if (material == null) {
                    return true;
                }

                String oldDropsId = zone.getMiningDropsId(material);
                DropSet drops = BeastMaster.LOOTS.getDropSet(oldDropsId);
                String dropsDescription = (drops != null) ? drops.getDescription()
                                                          : ChatColor.RED + oldDropsId + ChatColor.GOLD + " (not defined)";
                if (oldDropsId == null) {
                    sender.sendMessage(ChatColor.RED + "Zone " + zoneArg +
                                       " has no custom mining drops for " + material + "!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "When " + ChatColor.YELLOW + material +
                                       ChatColor.GOLD + " is broken in zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " loot will no longer drop from table " + dropsDescription +
                                       ChatColor.GOLD + ".");
                }
                zone.setMiningDropsId(material, null);
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("list-blocks")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " list-blocks <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                Set<Entry<Material, String>> allMiningDrops = zone.getAllMiningDrops();

                if (allMiningDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " has no configured block drops.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Block drops in zone " +
                                       ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + ": ");
                    for (Entry<Material, String> entry : allMiningDrops) {
                        Material material = entry.getKey();
                        String dropsId = entry.getValue();
                        DropSet drops = BeastMaster.LOOTS.getDropSet(dropsId);
                        String dropsDescription = (drops != null) ? drops.getDescription()
                                                                  : ChatColor.RED + dropsId + ChatColor.GOLD + " (not defined)";
                        sender.sendMessage(ChatColor.WHITE + material.toString() + ": " + dropsDescription);
                    }
                }
                return true;
            }
        }

        return false;
    } // onCommand
} // class BeastZoneExecutor