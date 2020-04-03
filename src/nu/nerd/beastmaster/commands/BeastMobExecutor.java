package nu.nerd.beastmaster.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropType;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.Util;
import nu.nerd.beastmaster.mobs.MobProperty;
import nu.nerd.beastmaster.mobs.MobType;

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
        super("beast-mob", "help",
              "add", "remove", "list",
              "info", "parent",
              "get", "set", "clear", "spawn", "statue");
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
                    Commands.invalidArguments(sender, getName() + " add <mob-id> <parent-id>");
                    return true;
                }

                String idArg = args[1];
                String parentIdArg = args[2];

                if (DropType.isDropType(idArg)) {
                    sender.sendMessage(ChatColor.RED + "You can't use The ID \"" + idArg + "\"; it is reserved.");
                    return true;
                }

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType != null) {
                    Commands.errorNotNull(sender, "mob type", idArg);
                    return true;
                }

                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item != null) {
                    sender.sendMessage(ChatColor.RED + "An item named \"" + idArg + "\" is already defined. " +
                                       "Mob types can't have the same ID as an existing item.");
                    return true;
                }

                MobType parentType = BeastMaster.MOBS.getMobType(parentIdArg);
                if (parentType == null) {
                    Commands.errorNull(sender, "parent type", parentIdArg);
                    sender.sendMessage(ChatColor.RED + "Run \"/beast-mob list\" to see valid parent types.");
                    return true;
                }

                mobType = new MobType(idArg, parentIdArg);
                BeastMaster.MOBS.addMobType(mobType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Added a new mob type: " + mobType.getShortDescription());
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <mob-id>");
                    return true;
                }

                String mobIdArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                BeastMaster.MOBS.removeMobType(mobType);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Removed mob type: " + mobType.getShortDescription());
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                List<String> predefinedMobs = BeastMaster.MOBS.getPredefinedMobTypes().stream()
                .map(MobType::getId).sorted().collect(Collectors.toList());
                sender.sendMessage(ChatColor.GOLD + "Predefined mob types: " +
                                   Util.alternateColours(predefinedMobs, ChatColor.GRAY + " ", ChatColor.WHITE, ChatColor.YELLOW));

                sender.sendMessage(ChatColor.GOLD + "Custom mob types:");
                ArrayList<MobType> customMobTypes = BeastMaster.MOBS.getCustomMobTypes();
                customMobTypes.sort((t1, t2) -> t1.getId().compareToIgnoreCase(t2.getId()));
                for (MobType mobType : customMobTypes) {
                    sender.sendMessage(mobType.getShortDescription());
                }
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <mob-id>");
                    return true;
                }

                String mobIdArg = args[1];
                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Mob type: " + ChatColor.YELLOW + mobType.getId());
                for (String propertyId : MobType.getAllPropertyIds()) {
                    showProperty(sender, mobType, propertyId, false);
                }
                return true;

            } else if (args[0].equals("get")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " get <mob-id> <property>");
                    listPropertyIds(sender);
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    listPropertyIds(sender);
                    return true;
                }

                showProperty(sender, mobType, propertyArg, true);
                return true;

            } else if (args[0].equals("set")) {
                if (args.length < 4) {
                    Commands.invalidArguments(sender, getName() + " set <mob-id> <property> <value>");
                    listPropertyIds(sender);
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];
                String valueArg = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                if (mobType.isPredefined() &&
                    MobType.getImmutablePredefinedPropertyNames().contains(propertyArg)) {
                    sender.sendMessage(ChatColor.RED + "You cannot change the " + propertyArg +
                                       " property of predefined mob types.");
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    listPropertyIds(sender);
                    return true;
                }

                Object value;
                try {
                    value = property.getType().parse(valueArg, sender, property.getId());
                    if (value == null) {
                        return true;
                    }
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + valueArg + " is not a valid " + property.getId() + " value.");
                    return true;
                }

                // Special case: prevent a mob being its own ancestor (infinite
                // recursion that hangs the main server thread).
                if (property.getId().equals("parent-type")) {
                    MobType newParent = BeastMaster.MOBS.getMobType((String) value);
                    if (mobType == newParent) {
                        sender.sendMessage(ChatColor.RED + "A mob type cannot be its own parent.");
                        return true;
                    } else {
                        HashSet<MobType> ancestors = new HashSet<>();
                        Util.transitiveClosure(ancestors, newParent,
                                               mt -> (mt.getParentType() != null ? Arrays.asList(mt.getParentType())
                                                                                 : Collections.EMPTY_LIST));
                        if (ancestors.contains(mobType)) {
                            sender.sendMessage(ChatColor.RED +
                                               "A mob type cannot be its own ancestor (parent, grandparent, etc). " +
                                               newParent.getId() + "'s ancestors already include " + mobType.getId() + ".");
                            return true;
                        }
                    }
                }

                property.setValue(value);
                sender.sendMessage(ChatColor.GOLD + mobType.getId() + ": " +
                                   ChatColor.YELLOW + property.getId() +
                                   ChatColor.WHITE + " = " +
                                   ChatColor.YELLOW + property.getFormattedValue());
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("clear")) {
                if (args.length != 3) {
                    Commands.invalidArguments(sender, getName() + " clear <mob-id> <property>");
                    return true;
                }

                String mobIdArg = args[1];
                String propertyArg = args[2];

                MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
                if (mobType == null) {
                    Commands.errorNull(sender, "mob type", mobIdArg);
                    return true;
                }

                MobProperty property = mobType.getProperty(propertyArg);
                if (property == null) {
                    Commands.errorNull(sender, "property", propertyArg);
                    return true;
                }

                property.setValue(null);
                sender.sendMessage(ChatColor.GOLD + mobType.getId() + ": " +
                                   ChatColor.YELLOW + property.getId() +
                                   ChatColor.WHITE + " = " +
                                   ChatColor.YELLOW + "unset");
                BeastMaster.CONFIG.save();
                return true;

            } else if (args[0].equals("spawn") || args[0].equals("statue")) {
                return onCommandSpawnOrStatue(sender, args);
            }
        }

        return false;
    } // onCommand

    // --------------------------------------------------------------------------
    /**
     * Parse the "spawn" and "statue" sub-commands.
     * 
     * Separated into its own method because it's such a long lump of code.
     * 
     * @param sender the command sender.
     * @param args the command arguments.
     * @return true if the command was handled by this method.
     */
    protected boolean onCommandSpawnOrStatue(CommandSender sender, String[] args) {
        final String USAGE = getName() + " " + args[0] + " <mob-id> here|there|<world> <x> <y> <z>";
        if (args.length != 3 && args.length != 6) {
            Commands.invalidArguments(sender, USAGE);
            return true;
        }

        String mobIdArg = args[1];
        String locFirstArg = args[2];

        MobType mobType = BeastMaster.MOBS.getMobType(mobIdArg);
        if (mobType == null) {
            Commands.errorNull(sender, "mob type", mobIdArg);
            return true;
        }

        Location spawnLoc = null;
        if (locFirstArg.equals("here") || locFirstArg.equals("there")) {
            if (args.length != 3) {
                Commands.invalidArguments(sender, USAGE);
                return true;
            }

            if (!isInGame(sender)) {
                return true;
            }
            Player player = (Player) sender;

            if (locFirstArg.equals("here")) {
                spawnLoc = player.getLocation();
            } else {
                // "spawn there" subcommand.
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
                    sender.sendMessage(ChatColor.RED + "You can't spawn a mob there. There is no solid block, or it's more than " +
                                       MAX_DISTANCE + " blocks away.");
                    return true;
                }

                // Offset the mob by a small fraction of a block in the
                // direction of the hit block face, to put it in a spawnable
                // gap.
                final double DELTA = 0.001;
                Vector hitPos = ray.getHitPosition();
                BlockFace hitFace = ray.getHitBlockFace();
                spawnLoc = new Location(world,
                    hitPos.getX() + hitFace.getModX() * DELTA,
                    hitPos.getY() + hitFace.getModY() * DELTA,
                    hitPos.getZ() + hitFace.getModZ() * DELTA);
            }
        } else {
            // Mob spawn location is <world> <x> <y> <z> parsed from args.
            if (args.length != 6) {
                Commands.invalidArguments(sender, USAGE);
                return true;
            }

            String worldArg = locFirstArg;
            String xArg = args[3], yArg = args[4], zArg = args[5];

            World world = Bukkit.getWorld(worldArg);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Invalid world name: " + worldArg + ".");
                return true;
            }

            Double x = Commands.parseNumber(xArg, Double::parseDouble, v -> true, null, () -> {
                sender.sendMessage(ChatColor.RED + "Invalid X coordinate: " + xArg);
            });
            if (x == null) {
                return true;
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
                return true;
            }
            Double z = Commands.parseNumber(zArg, Double::parseDouble, v -> true, null, () -> {
                sender.sendMessage(ChatColor.RED + "Invalid Z coordinate: " + zArg);
            });
            if (z == null) {
                return true;
            }

            spawnLoc = new Location(world, x, y, z);
        }

        boolean isStatue = args[0].equals("statue");
        LivingEntity mob = BeastMaster.PLUGIN.spawnMob(spawnLoc, mobType, false);
        if (mob != null) {
            mob.setAI(!isStatue);
            sender.sendMessage(ChatColor.GOLD + "Spawned " + ChatColor.YELLOW + mobType.getId() +
                               ChatColor.GOLD + (isStatue ? " statue" : "") + " at " +
                               ChatColor.YELLOW + Util.formatLocation(spawnLoc) +
                               ChatColor.GOLD + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to spawn a " + mobType.getId() +
                               (isStatue ? " statue" : "") + " at " + Util.formatLocation(spawnLoc) + ".");
            sender.sendMessage(ChatColor.RED + "Is there a WorldGuard region preventing that?");
        }
        return true;
    } // onCommandSpawnOrStatue

    // ------------------------------------------------------------------------
    /**
     * List all valid property IDs.
     * 
     * @param sender the command sender to message.
     */
    protected void listPropertyIds(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Valid property names are: " +
                           Util.alternateColours(MobType.getAllPropertyIds(), ChatColor.GRAY + " ", ChatColor.WHITE, ChatColor.YELLOW));
    }

    // ------------------------------------------------------------------------
    /**
     * Message the CommandSender the value of the specified mob property.
     * 
     * Where the property is unset, the inherited value is shown and the
     * ancestor MobType from which the property value was inherited is
     * indicated.
     * 
     * @param sender the command sender.
     * @param mobType the type of the mob.
     * @param propertyId the ID of the property whose value is shown.
     * @param showUnset if true, show the values of properties that have not
     *        been set (are null); otherwise don't show those.
     */
    protected void showProperty(CommandSender sender, MobType mobType,
                                String propertyId, boolean showUnset) {
        MobProperty property = mobType.getProperty(propertyId);
        MobProperty derivedProperty = mobType.getDerivedProperty(propertyId);

        if (derivedProperty.getValue() != null || showUnset) {
            // Show source of inherited properties only.
            String source = (property != derivedProperty) ? derivedProperty.getMobType().getId() + ": "
                                                          : "";
            sender.sendMessage(ChatColor.GOLD + property.getId() + ": " +
                               ChatColor.WHITE + source +
                               ChatColor.YELLOW + derivedProperty.getFormattedValue());
        }
    }
} // class BeastMobExecutor