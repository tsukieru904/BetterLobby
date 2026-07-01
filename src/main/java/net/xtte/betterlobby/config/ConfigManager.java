package net.xtte.betterlobby.config;

import net.xtte.betterlobby.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File configFile;
    private File messagesFile;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 載入或重新載入設定檔與語言檔 */
    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // 補齊預設值 (若使用者的設定檔缺少新選項，從內建資源補上)
        mergeDefaults(configFile, config, "config.yml");
        mergeDefaults(messagesFile, messages, "messages.yml");
    }

    private void mergeDefaults(File file, FileConfiguration loaded, String resourceName) {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) return;
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            loaded.setDefaults(defaults);
            loaded.options().copyDefaults(true);
            loaded.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("無法合併預設設定值: " + e.getMessage());
        }
    }

    // ---------------- 一般設定 ----------------

    public String getServerName() {
        return config.getString("server-name", "lobby-1");
    }

    public String getDefaultLobby() {
        return config.getString("default-lobby", "main");
    }

    // ---------------- 資料庫設定 ----------------

    public String getStorageType() {
        return config.getString("storage.type", "sqlite").toLowerCase();
    }

    public String getMysqlHost() {
        return config.getString("storage.mysql.host", "127.0.0.1");
    }

    public int getMysqlPort() {
        return config.getInt("storage.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("storage.mysql.database", "betterlobby");
    }

    public String getMysqlUsername() {
        return config.getString("storage.mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("storage.mysql.password", "");
    }

    public boolean isMysqlUseSSL() {
        return config.getBoolean("storage.mysql.useSSL", false);
    }

    public int getMysqlPoolSize() {
        return config.getInt("storage.mysql.pool-size", 10);
    }

    public int getSyncIntervalSeconds() {
        return config.getInt("storage.mysql.sync-interval-seconds", 30);
    }

    // ---------------- 傳送設定 ----------------

    public int getTeleportDelay() {
        return Math.max(0, config.getInt("teleport.delay", 5));
    }

    public boolean isCancelOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }

    public boolean isCancelOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }

    public boolean isInvulnerableDuringTeleport() {
        return config.getBoolean("teleport.invulnerable-during-teleport", false);
    }

    public boolean isActionbarCountdown() {
        return config.getBoolean("teleport.actionbar-countdown", true);
    }

    public boolean isCountdownSoundEnabled() {
        return config.getBoolean("teleport.countdown-sound.enabled", true);
    }

    public String getCountdownSoundName() {
        return config.getString("teleport.countdown-sound.sound", "UI_BUTTON_CLICK");
    }

    public float getCountdownSoundVolume() {
        return (float) config.getDouble("teleport.countdown-sound.volume", 1.0);
    }

    public float getCountdownSoundPitch() {
        return (float) config.getDouble("teleport.countdown-sound.pitch", 1.0);
    }

    public boolean isCountdownParticleEnabled() {
        return config.getBoolean("teleport.countdown-particle.enabled", true);
    }

    public String getCountdownParticleName() {
        return config.getString("teleport.countdown-particle.particle", "PORTAL");
    }

    public int getCountdownParticleCount() {
        return config.getInt("teleport.countdown-particle.count", 15);
    }

    public double getCountdownParticleOffsetX() {
        return config.getDouble("teleport.countdown-particle.offset-x", 0.5);
    }

    public double getCountdownParticleOffsetY() {
        return config.getDouble("teleport.countdown-particle.offset-y", 1.0);
    }

    public double getCountdownParticleOffsetZ() {
        return config.getDouble("teleport.countdown-particle.offset-z", 0.5);
    }

    public double getCountdownParticleSpeed() {
        return config.getDouble("teleport.countdown-particle.speed", 0.02);
    }

    public boolean isTeleportSoundEnabled() {
        return config.getBoolean("teleport.teleport-sound.enabled", true);
    }

    public String getTeleportSoundName() {
        return config.getString("teleport.teleport-sound.sound", "ENTITY_ENDERMAN_TELEPORT");
    }

    public float getTeleportSoundVolume() {
        return (float) config.getDouble("teleport.teleport-sound.volume", 1.0);
    }

    public float getTeleportSoundPitch() {
        return (float) config.getDouble("teleport.teleport-sound.pitch", 1.0);
    }

    // ---------------- 語言訊息 ----------------

    public String getPrefix() {
        return messages.getString("prefix", "&8[&dBetterLobby&8] &r");
    }

    /** 取得原始訊息 (未取代前綴) */
    public String getRaw(String path) {
        return messages.getString(path, "&c[缺少語言字串: " + path + "]");
    }

    /** 取得訊息並自動加上前綴、替換 {prefix} 及自訂變數 */
    public String get(String path, Map<String, String> placeholders) {
        String raw = getRaw(path);
        Map<String, String> all = new HashMap<>(placeholders);
        all.put("prefix", getPrefix());
        String replaced = MessageUtil.placeholders(raw, all);
        return MessageUtil.color(replaced);
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    /** 取得訊息但不自動加上前綴取代 (適合純文字，如 Action Bar 倒數) */
    public String getWithoutPrefixReplace(String path, Map<String, String> placeholders) {
        String raw = getRaw(path);
        return MessageUtil.color(MessageUtil.placeholders(raw, placeholders));
    }
}
