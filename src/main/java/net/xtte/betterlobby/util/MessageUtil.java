package net.xtte.betterlobby.util;

import org.bukkit.command.CommandSender;

import java.util.Map;

public class MessageUtil {

    /** 將 & 色碼轉換為 Minecraft 色碼 */
    public static String color(String input) {
        if (input == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
    }

    /** 取代訊息中的變數，格式如 {key} */
    public static String placeholders(String input, Map<String, String> values) {
        String result = input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /** 傳送已上色的訊息給玩家/主控台 */
    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(color(message));
    }
}
