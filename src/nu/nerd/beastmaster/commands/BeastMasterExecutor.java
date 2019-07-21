package nu.nerd.beastmaster.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;

// ----------------------------------------------------------------------------
/**
 * Executor for the /beastmaster command.
 */
public class BeastMasterExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public BeastMasterExecutor() {
        super("beastmaster", "help", "reload");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            BeastMaster.CONFIG.reload(true);
            sender.sendMessage(ChatColor.GOLD + BeastMaster.PLUGIN.getName() + " configuration reloaded.");
        }
        return true;
    }
} // class BeastMasterExecutor
