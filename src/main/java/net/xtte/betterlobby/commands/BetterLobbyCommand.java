package net.xtte.betterlobby.commands;

import net.xtte.betterlobby.BetterLobby;
import net.xtte.betterlobby.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BetterLobbyCommand implements CommandExecutor {

    private final BetterLobby plugin;

    public BetterLobbyCommand(BetterLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            MessageUtil.send(sender, "&c用法: /betterlobby reload");
            return true;
        }

        if (!sender.hasPermission("betterlobby.reload")) {
            MessageUtil.send(sender, plugin.getConfigManager().get("general.no-permission"));
            return true;
        }

        plugin.reload();
        MessageUtil.send(sender, plugin.getConfigManager().get("general.reload-success"));
        return true;
    }
}
