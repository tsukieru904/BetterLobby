package net.xtte.betterlobby.model;

/**
 * 一個純粹的座標點資料，不依賴 Bukkit 的 World 物件，
 * 因為跨伺服器情境下，來源伺服器不一定能取得目標伺服器的 World 實例。
 */
public record TeleportPoint(String world, double x, double y, double z, float yaw, float pitch) {
}
