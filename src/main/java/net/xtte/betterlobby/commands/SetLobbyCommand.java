package net.xtte.betterlobby.commands;

import net.xtte.betterlobby.BetterLobby;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.model.TeleportPoint;
import net.xtte.betterlobby.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetLobbyCommand implements CommandExecutor {

    private final BetterLobby plugin;

    public SetLobbyCommand(BetterLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().get("general.player-only"));
            return true;
        }

        if (!player.hasPermission("betterlobby.setlobby")) {
            MessageUtil.send(player, plugin.getConfigManager().get("general.no-permission"));
            return true;
        }

        String lobbyName = args.length >= 1 ? args[0] : plugin.getConfigManager().getDefaultLobby();
        String key = lobbyName.toLowerCase();

        Location loc = player.getLocation();
        TeleportPoint point = new TeleportPoint(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
        String serverName = plugin.getConfigManager().getServerName();

        LobbyLocation lobby = new LobbyLocation(lobbyName, serverName, point);

        plugin.getDbExecutor().execute(() -> {
            try {
                plugin.getStorageManager().saveLobby(lobbyName, serverName, point);
                plugin.getLobbyCache().put(key, lobby);
            } catch (Exception e) {
                plugin.getLogger().warning("儲存 Lobby 失敗: " + e.getMessage());
            }
        });

        MessageUtil.send(player, plugin.getConfigManager().get("setlobby.success",
                Map.of("lobby", lobbyName)));
        return true;
    }
}
