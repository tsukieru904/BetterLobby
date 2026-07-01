package net.xtte.betterlobby.commands;

import net.xtte.betterlobby.BetterLobby;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final BetterLobby plugin;

    public LobbyCommand(BetterLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().get("general.player-only"));
            return true;
        }

        if (!player.hasPermission("betterlobby.use")) {
            MessageUtil.send(player, plugin.getConfigManager().get("general.no-permission"));
            return true;
        }

        LobbyLocation lobby = plugin.getDefaultLobby();
        if (lobby == null) {
            MessageUtil.send(player, plugin.getConfigManager().get("lobby.not-found"));
            return true;
        }

        plugin.getTeleportManager().startTeleport(player, lobby);
        return true;
    }
}
