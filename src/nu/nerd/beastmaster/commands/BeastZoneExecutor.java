package nu.nerd.beastmaster.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.zones.Zone;

// /<command> replace-mob <zone-id> <entity-type> <loot-id>
// /<command> list-replacements <zone-id>

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
              "replace-mob", "list-replacements",
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

            } else if (args[0].equals("replace-mob")) {
                if (args.length != 4) {
                    Commands.invalidArguments(sender, getName() + " replace-mob <zone-id> <entity-type> <loot-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                String entityTypeArg = args[2];
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeArg.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    Commands.errorNull(sender, "entity type", entityTypeArg);
                    return true;
                }

                String lootIdArg = args[3];
                if (lootIdArg.equalsIgnoreCase("none")) {
                    zone.setMobReplacementDropSetId(entityType, null);
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " will no longer replace mobs of type " +
                                       ChatColor.YELLOW + entityType + ChatColor.GOLD + ".");
                } else {
                    zone.setMobReplacementDropSetId(entityType, lootIdArg);
                    boolean lootTableExists = (BeastMaster.LOOTS.getDropSet(lootIdArg) != null);
                    ChatColor lootTableColour = (lootTableExists ? ChatColor.YELLOW : ChatColor.RED);
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " will replace mobs of type " +
                                       ChatColor.YELLOW + entityType +
                                       ChatColor.GOLD + " according to loot table " +
                                       lootTableColour + lootIdArg + ChatColor.GOLD + ".");
                }

                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("list-replacements")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " list-replacements <zone-id>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                if (zone.getAllReplacedEntityTypes().isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " doesn't replace any mobs.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Loot tables replacing mobs in zone " +
                                       ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + ":");
                    List<EntityType> sortedTypes = zone.getAllReplacedEntityTypes().stream()
                    .sorted(Comparator.comparing(EntityType::name))
                    .collect(Collectors.toList());
                    for (EntityType entityType : sortedTypes) {
                        String lootId = zone.getMobReplacementDropSetId(entityType);
                        boolean lootTableExists = (BeastMaster.LOOTS.getDropSet(lootId) != null);
                        ChatColor lootTableColour = (lootTableExists ? ChatColor.YELLOW : ChatColor.RED);
                        sender.sendMessage(ChatColor.YELLOW + entityType.name() + ChatColor.WHITE + " -> " +
                                           lootTableColour + lootId);
                    }
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

                // Material name uppercase for Material.getMaterial() later.
                String materialArg = args[2].toUpperCase();
                Material material = Commands.parseMaterial(sender, materialArg);
                if (material == null) {
                    return true;
                }

                // Check that the material is an actual block and not just an
                // item type. Suggest corrected material.
                if (!material.isBlock()) {
                    sender.sendMessage(ChatColor.RED + material.name() + " is not a placeable block type.");
                    if (materialArg.equals("POTATO")) {
                        sender.sendMessage(ChatColor.RED + "Did you mean POTATOES instead?");
                    } else {
                        // General case: CARROT -> CARROTS, etc.
                        material = Material.getMaterial(materialArg + "S");
                        if (material != null) {
                            sender.sendMessage(ChatColor.RED + "Did you mean " + material + " instead?");
                        }
                    }
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
                    List<Entry<Material, String>> sortedMiningDrops = allMiningDrops.stream()
                    .sorted(Comparator.comparing(Entry<Material, String>::getKey))
                    .collect(Collectors.toList());
                    for (Entry<Material, String> entry : sortedMiningDrops) {
                        Material material = entry.getKey();
                        String dropsId = entry.getValue();
                        DropSet drops = BeastMaster.LOOTS.getDropSet(dropsId);
                        String dropsDescription = (drops != null) ? ChatColor.YELLOW + dropsId
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