package net.xtte.betterlobby.listeners;

import net.xtte.betterlobby.BetterLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 監聽玩家移動與受傷事件，依設定決定是否取消傳送倒數，
 * 或在倒數期間讓玩家保持無敵。
 */
public class GuardListener implements Listener {

    private final BetterLobby plugin;

    public GuardListener(BetterLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getTeleportManager().isTeleporting(player.getUniqueId())) return;
        if (!plugin.getConfigManager().isCancelOnMove()) return;
        if (player.hasPermission("betterlobby.bypass")) return;

        // 只忽略純視角旋轉，不忽略實際位移
        if (event.getFrom().distanceSquared(event.getTo()) < 0.0001) {
            return;
        }

        plugin.getTeleportManager().cancelTeleport(player, "teleport.cancelled-move");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // 傳送過程中若因其他插件觸發的傳送 (非本插件自身觸發)，一併取消倒數
        Player player = event.getPlayer();
        if (!plugin.getTeleportManager().isTeleporting(player.getUniqueId())) return;
        plugin.getTeleportManager().cancelTeleport(player, "teleport.cancelled-move");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getTeleportManager().isTeleporting(player.getUniqueId())) return;
        if (player.hasPermission("betterlobby.bypass")) return;

        if (plugin.getConfigManager().isInvulnerableDuringTeleport()) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfigManager().isCancelOnDamage()) {
            plugin.getTeleportManager().cancelTeleport(player, "teleport.cancelled-damage");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTeleportManager().cancelTeleportSilently(event.getPlayer().getUniqueId());
    }
}
