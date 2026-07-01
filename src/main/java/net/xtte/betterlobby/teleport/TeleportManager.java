package net.xtte.betterlobby.teleport;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.xtte.betterlobby.BetterLobby;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.model.TeleportPoint;
import net.xtte.betterlobby.util.MessageUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final BetterLobby plugin;
    private final Map<UUID, TeleportSession> activeSessions = new ConcurrentHashMap<>();

    public TeleportManager(BetterLobby plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public TeleportSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    /** 開始傳送流程 (由 /lobby 指令呼叫) */
    public void startTeleport(Player player, LobbyLocation lobby) {
        UUID uuid = player.getUniqueId();

        if (isTeleporting(uuid)) {
            MessageUtil.send(player, plugin.getConfigManager().get("teleport.already-teleporting"));
            return;
        }

        // 判斷玩家是否已經在目標 Lobby 上 (同伺服器且同世界/座標範圍內)
        if (isCurrentlyAtLobby(player, lobby)) {
            MessageUtil.send(player, plugin.getConfigManager().get("lobby.already-there",
                    Map.of("lobby", lobby.getName())));
            return;
        }

        int delay = plugin.getConfigManager().getTeleportDelay();

        if (delay <= 0) {
            // 無延遲，直接傳送，不顯示倒數、不播放粒子效果
            performTeleport(player, lobby);
            return;
        }

        TeleportSession session = new TeleportSession(lobby, player.getLocation().clone(), delay);
        activeSessions.put(uuid, session);

        tick(player, session);
    }

    private void tick(Player player, TeleportSession session) {
        UUID uuid = player.getUniqueId();

        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            TeleportSession current = activeSessions.get(uuid);
            if (current == null || !player.isOnline()) {
                if (scheduledTask != null) scheduledTask.cancel();
                return;
            }

            if (current.getSecondsLeft() <= 0) {
                activeSessions.remove(uuid);
                scheduledTask.cancel();
                performTeleport(player, current.getTarget());
                return;
            }

            // 顯示倒數 Action Bar (動態數字會隨秒數遞減更新)
            if (plugin.getConfigManager().isActionbarCountdown()) {
                String msg = plugin.getConfigManager().getWithoutPrefixReplace("teleport.countdown",
                        Map.of("seconds", String.valueOf(current.getSecondsLeft()),
                                "prefix", plugin.getConfigManager().getPrefix()));
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
            }

            // 倒數音效
            if (plugin.getConfigManager().isCountdownSoundEnabled()) {
                playSound(player, plugin.getConfigManager().getCountdownSoundName(),
                        plugin.getConfigManager().getCountdownSoundVolume(),
                        plugin.getConfigManager().getCountdownSoundPitch());
            }

            // 倒數粒子效果 (環繞玩家)
            if (plugin.getConfigManager().isCountdownParticleEnabled()) {
                spawnParticle(player);
            }

            current.decrement();
        }, () -> activeSessions.remove(uuid), 1L, 20L);

        session.setTask(task);
    }

    /** 因移動、受傷或斷線而取消傳送 */
    public void cancelTeleport(Player player, String messageKey) {
        UUID uuid = player.getUniqueId();
        TeleportSession session = activeSessions.remove(uuid);
        if (session == null) return;
        session.cancelTask();
        if (messageKey != null) {
            MessageUtil.send(player, plugin.getConfigManager().get(messageKey));
        }
    }

    public void cancelTeleportSilently(UUID uuid) {
        TeleportSession session = activeSessions.remove(uuid);
        if (session != null) {
            session.cancelTask();
        }
    }

    /** 實際執行傳送 (同伺服器直接 teleport，跨伺服器則透過代理轉移) */
    private void performTeleport(Player player, LobbyLocation lobby) {
        String currentServer = plugin.getConfigManager().getServerName();

        if (lobby.getServer().equalsIgnoreCase(currentServer)) {
            teleportLocally(player, lobby.getPoint());
            MessageUtil.send(player, plugin.getConfigManager().get("teleport.success",
                    Map.of("lobby", lobby.getName())));
        } else {
            // 跨伺服器：先寫入待處理傳送紀錄，再請求代理轉移玩家
            UUID uuid = player.getUniqueId();
            TeleportPoint point = lobby.getPoint();
            plugin.getDbExecutor().execute(() -> {
                try {
                    plugin.getStorageManager().addPendingTeleport(uuid, point);
                } catch (Exception e) {
                    plugin.getLogger().warning("寫入跨伺服器傳送紀錄失敗: " + e.getMessage());
                }
            });
            MessageUtil.send(player, plugin.getConfigManager().get("teleport.connecting",
                    Map.of("server", lobby.getServer())));
            plugin.getBungeeMessenger().connect(player, lobby.getServer());
        }
    }

    private void teleportLocally(Player player, TeleportPoint point) {
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            plugin.getLogger().warning("找不到世界 '" + point.world() + "'，無法傳送玩家 " + player.getName());
            MessageUtil.send(player, plugin.getConfigManager().get("general.unknown-error"));
            return;
        }
        Location location = new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        player.teleportAsync(location).thenAccept(success -> {
            if (success && plugin.getConfigManager().isTeleportSoundEnabled()) {
                playSound(player, plugin.getConfigManager().getTeleportSoundName(),
                        plugin.getConfigManager().getTeleportSoundVolume(),
                        plugin.getConfigManager().getTeleportSoundPitch());
            }
        });
    }

    /** 玩家透過代理轉移到本伺服器後，處理待處理的傳送紀錄 */
    public void handlePendingTeleport(Player player) {
        if (!"mysql".equalsIgnoreCase(plugin.getConfigManager().getStorageType())) {
            return;
        }
        UUID uuid = player.getUniqueId();
        plugin.getDbExecutor().execute(() -> {
            try {
                plugin.getStorageManager().pollPendingTeleport(uuid).ifPresent(point ->
                        player.getScheduler().run(plugin, task -> {
                            teleportLocally(player, point);
                            MessageUtil.send(player, plugin.getConfigManager().get("teleport.success",
                                    Map.of("lobby", plugin.getConfigManager().getDefaultLobby())));
                        }, () -> {
                        }));
            } catch (Exception e) {
                plugin.getLogger().warning("讀取跨伺服器傳送紀錄失敗: " + e.getMessage());
            }
        });
    }

    private boolean isCurrentlyAtLobby(Player player, LobbyLocation lobby) {
        String currentServer = plugin.getConfigManager().getServerName();
        if (!lobby.getServer().equalsIgnoreCase(currentServer)) return false;

        TeleportPoint point = lobby.getPoint();
        World world = Bukkit.getWorld(point.world());
        if (world == null || !player.getWorld().equals(world)) return false;

        return player.getLocation().distanceSquared(new Location(world, point.x(), point.y(), point.z())) < 1.0;
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("無效的音效名稱: " + soundName);
        }
    }

    private void spawnParticle(Player player) {
        try {
            Particle particle = Particle.valueOf(plugin.getConfigManager().getCountdownParticleName().toUpperCase());
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(
                    particle,
                    loc,
                    plugin.getConfigManager().getCountdownParticleCount(),
                    plugin.getConfigManager().getCountdownParticleOffsetX(),
                    plugin.getConfigManager().getCountdownParticleOffsetY(),
                    plugin.getConfigManager().getCountdownParticleOffsetZ(),
                    plugin.getConfigManager().getCountdownParticleSpeed()
            );
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("無效的粒子效果名稱: " + plugin.getConfigManager().getCountdownParticleName());
        }
    }

    public void shutdown() {
        activeSessions.values().forEach(TeleportSession::cancelTask);
        activeSessions.clear();
    }
}
