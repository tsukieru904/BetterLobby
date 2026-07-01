package net.xtte.betterlobby.commands;

import net.xtte.betterlobby.BetterLobby;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LobbyCommand implements CommandExecutor, TabCompleter {

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

        String lobbyName = args.length >= 1 ? args[0] : plugin.getConfigManager().getDefaultLobby();

        LobbyLocation lobby = plugin.getLobbyCache().get(lobbyName.toLowerCase());
        if (lobby == null) {
            MessageUtil.send(player, plugin.getConfigManager().get("lobby.not-found",
                    Map.of("lobby", lobbyName)));
            return true;
        }

        plugin.getTeleportManager().startTeleport(player, lobby);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return plugin.getLobbyCache().keySet().stream()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
