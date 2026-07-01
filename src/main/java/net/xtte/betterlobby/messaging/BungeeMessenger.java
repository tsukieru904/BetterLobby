package net.xtte.betterlobby.messaging;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 透過 "BungeeCord" 插件訊息頻道請求代理伺服器 (Velocity) 將玩家轉移到另一台後端伺服器。
 * Velocity 預設會相容此舊版頻道名稱 (bungee plugin message channel)，
 * 因此不需要額外在 Velocity 安裝插件即可使用 Connect 子頻道。
 */
public class BungeeMessenger implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private final Plugin plugin;

    public BungeeMessenger(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }

    /** 請求代理伺服器將玩家轉移到指定的後端伺服器名稱 */
    public void connect(Player player, String targetServer) {
        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            player.sendPluginMessage(plugin, CHANNEL, byteArray.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("傳送跨伺服器訊息失敗: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // 目前不需要處理來自代理伺服器的回應訊息，保留擴充用
    }
}
