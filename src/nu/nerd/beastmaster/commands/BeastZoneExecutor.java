package nu.nerd.beastmaster.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.BeastMaster;
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
        super("beast-zone", "help", "add", "remove", "list");
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
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " add <id> <world>");
                    return true;
                }

                String zoneName = args[1];
                String worldName = args[2];
                Zone zone = BeastMaster.ZONES.getZone(zoneName);
                if (zone != null) {
                    sender.sendMessage(ChatColor.RED + "A zone named " + zoneName + " already exists!");
                    return true;
                }
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "There is no world named " + worldName + "!");
                    return true;
                }

                zone = new Zone(args[1], world);
                BeastMaster.ZONES.addZone(zone);
                BeastMaster.ZONES.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
                sender.sendMessage(ChatColor.GOLD + "Added new zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + " in world " +
                                   ChatColor.YELLOW + zone.getWorld().getName() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove <id>");
                    return true;
                }

                String id = args[1];
                Zone zone = BeastMaster.ZONES.getZone(id);
                if (zone == null) {
                    sender.sendMessage(ChatColor.RED + "There is no zone named \"" + id + "\".");
                    return true;
                }

                BeastMaster.ZONES.removeZone(zone);
                BeastMaster.ZONES.save(BeastMaster.PLUGIN.getConfig(), BeastMaster.PLUGIN.getLogger());
                sender.sendMessage(ChatColor.GOLD + "Removed zone " +
                                   ChatColor.YELLOW + zone.getId() +
                                   ChatColor.GOLD + ".");
                return true;

            } else if (args[0].equals("list")) {
                if (args.length > 1) {
                    sender.sendMessage(ChatColor.RED + "Too many arguments. Usage: /" + getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Zones:");
                for (Zone zone : BeastMaster.ZONES.getZones()) {
                    sender.sendMessage(zone.getDescription());
                }
                return true;
            }
        }

        return false;
    } // onCommand
} // class BeastZoneExecutor