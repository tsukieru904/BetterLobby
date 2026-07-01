package net.xtte.betterlobby.storage;

import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.model.TeleportPoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 資料儲存介面，SQLite 與 MySQL 皆實作此介面。
 * 所有方法皆為同步阻塞呼叫，呼叫端須自行丟到非主執行緒執行，
 * 避免卡住伺服器主執行緒（Bukkit 排程）。
 */
public interface StorageManager {

    /** 初始化連線與資料表 */
    void init() throws Exception;

    /** 關閉連線 */
    void close();

    /** 新增或更新預設 Lobby 資料 */
    void saveDefaultLobby(String server, TeleportPoint point) throws Exception;

    /** 取得預設 Lobby 資料 */
    Optional<LobbyLocation> getDefaultLobby() throws Exception;

    /**
     * 新增一筆待處理的跨伺服器傳送紀錄。
     * 玩家透過代理伺服器 (Velocity) 切換到目標伺服器後，
     * 目標伺服器會在玩家加入時查詢這筆紀錄並執行實際傳送。
     * 僅 MySQL 模式下有實際跨伺服器效果。
     */
    void addPendingTeleport(UUID uuid, TeleportPoint point) throws Exception;

    /** 取得並移除一筆待處理的傳送紀錄 */
    Optional<TeleportPoint> pollPendingTeleport(UUID uuid) throws Exception;
}
