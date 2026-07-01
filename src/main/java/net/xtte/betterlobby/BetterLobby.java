package net.xtte.betterlobby;

import net.xtte.betterlobby.commands.BetterLobbyCommand;
import net.xtte.betterlobby.commands.LobbyCommand;
import net.xtte.betterlobby.commands.SetLobbyCommand;
import net.xtte.betterlobby.config.ConfigManager;
import net.xtte.betterlobby.listeners.GuardListener;
import net.xtte.betterlobby.listeners.JoinListener;
import net.xtte.betterlobby.messaging.BungeeMessenger;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.storage.MySQLStorage;
import net.xtte.betterlobby.storage.SQLiteStorage;
import net.xtte.betterlobby.storage.StorageManager;
import net.xtte.betterlobby.teleport.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BetterLobby extends JavaPlugin {

    private ConfigManager configManager;
    private StorageManager storageManager;
    private BungeeMessenger bungeeMessenger;
    private TeleportManager teleportManager;

    private final Map<String, LobbyLocation> lobbyCache = new ConcurrentHashMap<>();
    private ExecutorService dbExecutor;
    private BukkitTask syncTask;

    @Override
    public void onEnable() {
        dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "BetterLobby-Storage");
            thread.setDaemon(true);
            return thread;
        });

        configManager = new ConfigManager(this);
        configManager.load();

        if (!initStorage()) {
            getLogger().severe("資料庫初始化失敗，插件將停用！");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        loadLobbyCacheSync();

        bungeeMessenger = new BungeeMessenger(this);
        bungeeMessenger.register();

        teleportManager = new TeleportManager(this);

        LobbyCommand lobbyCommand = new LobbyCommand(this);
        getCommand("lobby").setExecutor(lobbyCommand);
        getCommand("lobby").setTabCompleter(lobbyCommand);
        getCommand("setlobby").setExecutor(new SetLobbyCommand(this));
        getCommand("betterlobby").setExecutor(new BetterLobbyCommand(this));

        Bukkit.getPluginManager().registerEvents(new GuardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        startSyncTaskIfNeeded();

        getLogger().info("BetterLobby 已啟用！儲存方式: " + configManager.getStorageType());
    }

    @Override
    public void onDisable() {
        if (syncTask != null) {
            syncTask.cancel();
        }
        if (teleportManager != null) {
            teleportManager.shutdown();
        }
        if (bungeeMessenger != null) {
            bungeeMessenger.unregister();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdownNow();
        }
    }

    /** 重新載入設定檔、語言檔、資料庫連線與 Lobby 快取 */
    public void reload() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }

        configManager.load();

        if (storageManager != null) {
            storageManager.close();
        }
        initStorage();
        loadLobbyCacheSync();
        startSyncTaskIfNeeded();
    }

    private boolean initStorage() {
        try {
            String type = configManager.getStorageType();
            if ("mysql".equalsIgnoreCase(type)) {
                storageManager = new MySQLStorage(
                        configManager.getMysqlHost(),
                        configManager.getMysqlPort(),
                        configManager.getMysqlDatabase(),
                        configManager.getMysqlUsername(),
                        configManager.getMysqlPassword(),
                        configManager.isMysqlUseSSL(),
                        configManager.getMysqlPoolSize()
                );
            } else {
                storageManager = new SQLiteStorage(getDataFolder());
            }
            storageManager.init();
            return true;
        } catch (Exception e) {
            getLogger().severe("初始化資料庫時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void loadLobbyCacheSync() {
        lobbyCache.clear();
        try {
            List<LobbyLocation> all = storageManager.getAllLobbies();
            for (LobbyLocation lobby : all) {
                lobbyCache.put(lobby.getName().toLowerCase(), lobby);
            }
            getLogger().info("已載入 " + all.size() + " 筆 Lobby 資料。");
        } catch (Exception e) {
            getLogger().warning("載入 Lobby 快取失敗: " + e.getMessage());
        }
    }

    /** 定期從資料庫重新整理 Lobby 快取，僅 MySQL 模式下需要 (讓其他伺服器新增/修改的 Lobby 能同步過來) */
    private void startSyncTaskIfNeeded() {
        if (!"mysql".equalsIgnoreCase(configManager.getStorageType())) {
            return;
        }
        long intervalTicks = Math.max(20L, configManager.getSyncIntervalSeconds() * 20L);
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                List<LobbyLocation> all = storageManager.getAllLobbies();
                Map<String, LobbyLocation> updated = new ConcurrentHashMap<>();
                for (LobbyLocation lobby : all) {
                    updated.put(lobby.getName().toLowerCase(), lobby);
                }
                lobbyCache.clear();
                lobbyCache.putAll(updated);
            } catch (Exception e) {
                getLogger().warning("同步 Lobby 快取失敗: " + e.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    // ---------------- Getters ----------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public BungeeMessenger getBungeeMessenger() {
        return bungeeMessenger;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public Map<String, LobbyLocation> getLobbyCache() {
        return lobbyCache;
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }
}
