package net.xtte.betterlobby.listeners;

import net.xtte.betterlobby.BetterLobby;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final BetterLobby plugin;

    public JoinListener(BetterLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 玩家可能是透過代理伺服器從其他分流轉移過來，檢查是否有待處理的傳送紀錄
        plugin.getTeleportManager().handlePendingTeleport(event.getPlayer());
    }
}
