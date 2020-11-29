package nu.nerd.beastmaster.commands;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.Util;
import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.Lexer;
import nu.nerd.beastmaster.zones.ParseError;
import nu.nerd.beastmaster.zones.Parser;
import nu.nerd.beastmaster.zones.Zone;
import nu.nerd.beastmaster.zones.ZonePredicate;

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
        super("beast-zone", "help", "language",
            "add", "remove", "parent", "spec", "list", "get",
            "replace-mob", "list-replacements",
            "add-block", "remove-block", "list-blocks",
            "inherit-replacements", "inherit-blocks");
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
            if (args[0].equals("language")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " language");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Operators, in descending order of precedence:");
                sender.sendMessage(ChatColor.YELLOW + "(x)" + ChatColor.WHITE + " - Parentheses can be used to specify evaluation order.");
                sender.sendMessage(ChatColor.YELLOW + "!x" + ChatColor.WHITE + " - True if expression x is false (\"NOT x\").");
                sender.sendMessage(ChatColor.YELLOW + "x & y" + ChatColor.WHITE + " - True if both expressions x and y are true.");
                sender.sendMessage(ChatColor.YELLOW + "x | y" + ChatColor.WHITE + " - True if either expression x or y is true.");
                sender.sendMessage(ChatColor.YELLOW + "x ^ y" + ChatColor.WHITE
                                   + " - True if either expression x or y is true, but not both (\"exclusive OR\").");

                sender.sendMessage(ChatColor.GOLD + "Predicates:");
                for (ZonePredicate pred : ZonePredicate.values()) {
                    sender.sendMessage(ChatColor.YELLOW + pred.name().toLowerCase() +
                                       ChatColor.WHITE + "(" + pred.getParameters().getParameterNames() + ChatColor.WHITE + ") - " +
                                       ChatColor.WHITE + pred.getHelp());
                }
                sender.sendMessage(ChatColor.GOLD + "Example:");
                sender.sendMessage(ChatColor.YELLOW + "circle(0,0,500) & (biome(\"RIVER\") | biome(\"OCEAN\")) & !wg(\"*\")");
                return true;

            } else if (args[0].equals("add")) {
                if (args.length < 4) {
                    Commands.invalidArguments(sender, getName() + " add <zone-id> <parent-id> <specification>");
                    return true;
                }

                String zoneArg = args[1];
                String parentArg = args[2];
                String specArg = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone != null) {
                    Commands.errorNotNull(sender, "zone", zoneArg);
                    return true;
                }

                Zone parent = BeastMaster.ZONES.getZone(parentArg);
                if (parent == null) {
                    Commands.errorNull(sender, "parent zone", parentArg);
                    return true;
                }

                Expression expression = parseZoneSpecification(sender, specArg);
                if (expression != null) {
                    zone = new Zone(zoneArg, parent, expression);
                    BeastMaster.ZONES.addZone(zone);
                    BeastMaster.CONFIG.save();
                    sender.sendMessage(ChatColor.GOLD + "Added zone " + zone.getDescription() +
                                       ChatColor.GOLD + ".");
                }

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

                if (zone.isRoot()) {
                    sender.sendMessage(ChatColor.RED + "You cannot remove root zones!");
                    return true;
                }

                BeastMaster.ZONES.removeZone(zone);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("parent")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " spec <zone-id> <specification>");
                    return true;
                }

                String zoneArg = args[1];
                String parentArg = args[2];

                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                if (zone.isRoot()) {
                    sender.sendMessage(ChatColor.RED + "You cannot change the parent root zones!");
                    return true;
                }

                Zone newParentZone = BeastMaster.ZONES.getZone(parentArg);
                if (newParentZone == null) {
                    Commands.errorNull(sender, "zone", parentArg);
                    return true;
                }

                Zone oldParentZone = zone.getParent();
                zone.setParent(newParentZone);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "The parent of " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + " was changed from " +
                                   ChatColor.YELLOW + oldParentZone.getId() +
                                   ChatColor.GOLD + " to " +
                                   ChatColor.YELLOW + newParentZone.getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("spec")) {
                if (args.length < 3) {
                    Commands.invalidArguments(sender, getName() + " spec <zone-id> <specification>");
                    return true;
                }

                String zoneArg = args[1];
                String specArg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                if (zone.isRoot()) {
                    sender.sendMessage(ChatColor.RED + "You cannot change the Zone Specification of root zones!");
                    return true;
                }

                Expression expression = parseZoneSpecification(sender, specArg);
                if (expression != null) {
                    String oldSpecification = zone.getSpecification();
                    zone.setExpression(expression);
                    BeastMaster.CONFIG.save();
                    sender.sendMessage(ChatColor.GOLD + "Redefined zone " + zone.getDescription() +
                                       ChatColor.GOLD + ".");
                    sender.sendMessage(ChatColor.GOLD + "The old specification was " +
                                       ChatColor.WHITE + oldSpecification +
                                       ChatColor.GOLD + ".");
                }
                return true;

            } else if (args[0].equals("list")) {
                if (args.length < 1 || args.length > 2) {
                    Commands.invalidArguments(sender, getName() + " list [ <parent-id> ]");
                    return true;
                }

                if (args.length == 2) {
                    String parentArg = args[1];
                    Zone parentZone = BeastMaster.ZONES.getZone(parentArg);
                    if (parentZone == null) {
                        Commands.errorNull(sender, "zone", parentArg);
                        return true;
                    }

                    sender.sendMessage(ChatColor.GOLD + "Zone: " + parentZone.getDescription());
                    int childCount = parentZone.children().size();
                    if (childCount > 0) {
                        sender.sendMessage(ChatColor.GOLD + "Children:");
                        for (int i = 0; i < childCount; ++i) {
                            sender.sendMessage(ChatColor.WHITE + "(" + (i + 1) + ") " +
                                               parentZone.children().get(i).getDescription());
                        }
                    }

                } else {
                    sender.sendMessage(ChatColor.GOLD + "Root zones:");
                    for (Zone zone : BeastMaster.ZONES.getRootZones()) {
                        sender.sendMessage(zone.getDescription());
                    }
                }
                return true;

            } else if (args[0].equals("get")) {
                onCommandGet(sender, args);
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

                Set<Entry<EntityType, String>> allReplacements = zone.getAllReplacedEntityTypes(true).entrySet();
                if (allReplacements.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " doesn't replace any mobs.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Loot tables replacing mobs in zone " +
                                       ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + ":");
                    List<Entry<EntityType, String>> sortedReplacements = allReplacements.stream()
                        .sorted(Comparator.comparing(e -> e.getKey().name()))
                        .collect(Collectors.toList());
                    for (Entry<EntityType, String> replacement : sortedReplacements) {
                        EntityType entityType = replacement.getKey();
                        String dropsId = replacement.getValue();

                        boolean dropSetExists = (BeastMaster.LOOTS.getDropSet(dropsId) != null);
                        String dropsDescription = (dropSetExists) ? ChatColor.GREEN + dropsId
                                                                  : ChatColor.RED + dropsId + ChatColor.WHITE + " (not defined)";

                        Zone definingZone = zone.getMobReplacementDefiningZone(entityType);
                        String inheritance = (definingZone == zone) ? ""
                                                                    : ChatColor.WHITE + " inherited from " +
                                                                      ChatColor.YELLOW + definingZone.getId();
                        sender.sendMessage(ChatColor.YELLOW + entityType.name() + ChatColor.WHITE + " -> " +
                                           dropsDescription + inheritance);
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

                String oldDropsId = zone.getMiningDropsId(material, false);
                DropSet drops = BeastMaster.LOOTS.getDropSet(oldDropsId);
                String dropsDescription = (drops != null) ? drops.getDescription()
                                                          : ChatColor.RED + oldDropsId + ChatColor.WHITE + " (not defined)";
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

                Set<Entry<Material, String>> allMiningDrops = zone.getAllMiningDrops(true).entrySet();
                if (allMiningDrops.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + " has no configured block drops.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Block drops in zone " +
                                       ChatColor.YELLOW + zoneArg +
                                       ChatColor.GOLD + ": ");
                    List<Entry<Material, String>> sortedMiningDrops = allMiningDrops.stream()
                        .sorted(Comparator.comparing(e -> e.getKey().name()))
                        .collect(Collectors.toList());
                    for (Entry<Material, String> entry : sortedMiningDrops) {

                        Material material = entry.getKey();
                        String dropsId = entry.getValue();
                        DropSet drops = BeastMaster.LOOTS.getDropSet(dropsId);
                        String dropsDescription = (drops != null) ? ChatColor.GREEN + dropsId
                                                                  : ChatColor.RED + dropsId + ChatColor.WHITE + " (not defined)";

                        Zone definingZone = zone.getMiningDropsDefiningZone(material);
                        String inheritance = (definingZone == zone) ? ""
                                                                    : ChatColor.WHITE + " inherited from " +
                                                                      ChatColor.YELLOW + definingZone.getId();

                        sender.sendMessage(ChatColor.YELLOW + material.toString() +
                                           ChatColor.WHITE + " -> " + dropsDescription + inheritance);
                    }
                }
                return true;

            } else if (args[0].equals("inherit-replacements")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " inherit-replacements <zone-id> <yes-or-no>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                if (zone.isRoot()) {
                    sender.sendMessage(ChatColor.RED + "Root zones can't inherit mob replacements (they have no parent)!");
                    return true;
                }

                String yesNoArg = args[2];
                Boolean inheritsReplacements = Commands.parseBoolean(sender, yesNoArg, "inherits replacements");
                if (inheritsReplacements == null) {
                    return true;
                }

                zone.setInheritsReplacements(inheritsReplacements);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                   ChatColor.GOLD + " will now " +
                                   ChatColor.YELLOW + (inheritsReplacements ? "inherit" : "not inherit") +
                                   ChatColor.GOLD + " mob replacements from parent zone " +
                                   ChatColor.YELLOW + zone.getParent().getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("inherit-blocks")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " inherit-blocks <zone-id> <yes-or-no>");
                    return true;
                }

                String zoneArg = args[1];
                Zone zone = BeastMaster.ZONES.getZone(zoneArg);
                if (zone == null) {
                    Commands.errorNull(sender, "zone", zoneArg);
                    return true;
                }

                if (zone.isRoot()) {
                    sender.sendMessage(ChatColor.RED + "Root zones can't inherit mining drops (they have no parent)!");
                    return true;
                }

                String yesNoArg = args[2];
                Boolean inheritsBlocks = Commands.parseBoolean(sender, yesNoArg, "inherits blocks");
                if (inheritsBlocks == null) {
                    return true;
                }

                zone.setInheritsBlocks(inheritsBlocks);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Zone " + ChatColor.YELLOW + zoneArg +
                                   ChatColor.GOLD + " will now " +
                                   ChatColor.YELLOW + (inheritsBlocks ? "inherit" : "not inherit") +
                                   ChatColor.GOLD + " mining drops from parent zone " +
                                   ChatColor.YELLOW + zone.getParent().getId() +
                                   ChatColor.GOLD + ".");
                return true;

            }
        }

        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Parse the "get" sub-command.
     *
     * Separated into its own method because it's such a long lump of code.
     *
     * @param sender the command sender.
     * @param args   the command arguments.
     */
    protected void onCommandGet(CommandSender sender, String[] args) {
        final String USAGE = getName() + " get (here | there | <world> <x> <y> <z>)";
        if (args.length != 2 && args.length != 5) {
            Commands.invalidArguments(sender, USAGE);
            return;
        }

        String locFirstArg = args[1];
        Location testLoc = null;
        if (locFirstArg.equals("here") || locFirstArg.equals("there")) {
            if (args.length != 2) {
                Commands.invalidArguments(sender, USAGE);
                return;
            }

            if (!isInGame(sender)) {
                return;
            }
            Player player = (Player) sender;

            if (locFirstArg.equals("here")) {
                testLoc = player.getLocation();
            } else {
                // "get there" subcommand.
                final int MAX_DISTANCE = 64;
                // Note: vitally important to start ray from eye.
                Location playerLoc = player.getEyeLocation();
                World world = playerLoc.getWorld();
                RayTraceResult ray = world.rayTraceBlocks(playerLoc,
                                                          playerLoc.getDirection(),
                                                          MAX_DISTANCE,
                                                          FluidCollisionMode.NEVER,
                                                          true);
                if (ray == null) {
                    sender.sendMessage(ChatColor.RED + "You can't get the biome there. There is no solid block, or it's more than " +
                                       MAX_DISTANCE + " blocks away.");
                    return;
                }

                testLoc = ray.getHitBlock().getLocation();
            }
        } else {
            // Test location is <world> <x> <y> <z> parsed from args.
            if (args.length != 5) {
                Commands.invalidArguments(sender, USAGE);
                return;
            }

            String worldArg = locFirstArg;
            String xArg = args[2], yArg = args[3], zArg = args[4];

            World world = Bukkit.getWorld(worldArg);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Invalid world name: " + worldArg + ".");
                return;
            }

            Double x = Commands.parseNumber(xArg, Double::parseDouble, v -> true, null, () -> {
                sender.sendMessage(ChatColor.RED + "Invalid X coordinate: " + xArg);
            });
            if (x == null) {
                return;
            }
            // Allow Y coordinates way up in the sky, for funsies.
            Double y = Commands.parseNumber(yArg, Double::parseDouble, v -> (v >= 0 && v <= 512),
                                            () -> {
                                                sender.sendMessage(ChatColor.RED + "The Y coordinate must be in the range [0,512].");
                                            },
                                            () -> {
                                                sender.sendMessage(ChatColor.RED + "Invalid Y coordinate: " + yArg);
                                            });
            if (y == null) {
                return;
            }
            Double z = Commands.parseNumber(zArg, Double::parseDouble, v -> true, null, () -> {
                sender.sendMessage(ChatColor.RED + "Invalid Z coordinate: " + zArg);
            });
            if (z == null) {
                return;
            }

            testLoc = new Location(world, x, y, z);
        }

        Zone zone = BeastMaster.ZONES.getZone(testLoc);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED
                               + "No zone found. This in an error: it should always be possible to find the zone at a location."
                               + " Please report this to the plugin author.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "The zone at " +
                               ChatColor.YELLOW + Util.formatLocation(testLoc) +
                               ChatColor.GOLD + " is " +
                               ChatColor.YELLOW + zone.getDescription() + ChatColor.GOLD + ".");
        }
        return;
    } // onCommandGet

    // ------------------------------------------------------------------------
    /**
     * Return the Expression resulting from parsing a Zone Specification.
     *
     * If there is a parse error, it is indicated to the command sender.
     *
     * @param sender the command sender.
     * @param spec   the specification.
     * @return the Expression, or null on error.
     */
    protected static Expression parseZoneSpecification(CommandSender sender, String spec) {
        try {
            Lexer lexer = new Lexer(spec);
            Parser parser = new Parser(lexer);
            return parser.parse();

        } catch (ParseError ex) {
            int errorColumn = ex.getToken().getColumn();
            int errorStart = errorColumn - 1;
            int errorEnd = Math.min(spec.length(), errorStart + ex.getToken().getLength());
            sender.sendMessage(ChatColor.RED + "Error at column " + errorColumn + ": " + ex.getMessage() + ".");
            sender.sendMessage(ChatColor.RED + "In specification: " +
                               ChatColor.WHITE + spec.substring(0, errorStart) +
                               ChatColor.RED + ">" +
                               spec.substring(errorStart, errorEnd) +
                               ChatColor.RED + "<" +
                               ChatColor.WHITE + spec.substring(errorEnd));
            return null;
        }
    }
} // class BeastZoneExecutor