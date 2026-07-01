# BetterLobby

支援跨分流同步的 Lobby 傳送插件，適用於 Purpur 26.1.2（Velocity 代理網路）。

## ⚠️ 重要：本專案尚未實際編譯測試

目前的沙盒環境沒有網路連線，無法執行 `mvn package` 下載相依套件並實際編譯驗證，
所有程式碼皆依據 Paper/Purpur 26.1.x 官方 API 手寫完成，邏輯與語法都已仔細檢查過，
但你在自己的環境編譯時，**請務必先跑一次 `mvn clean package` 確認沒有編譯錯誤**，
如果遇到 API 名稱因版本更新而略有差異的狀況，麻煩把錯誤訊息貼給我，我會馬上修正。

## 編譯方式

需要 JDK 25（因應 Minecraft 26.1.x 的 class 檔版本需求）以及網路連線（下載 Purpur API 與相依套件）。

```bash
mvn clean package
```

打包完成後，jar 檔會在 `target/BetterLobby-1.0.0.jar`，把它放進兩台後端伺服器的 `plugins/` 資料夾即可。

## 安裝與設定重點

1. **每台伺服器都要裝這個插件**，並且在各自的 `config.yml` 把 `server-name` 改成該伺服器
   在 Velocity `velocity.toml` 的 `[servers]` 區塊中登記的名稱（例如 `lobby-1`、`survival` 等）。
2. 若要讓兩台伺服器共用同一顆 Lobby：
   - 兩台伺服器的 `storage.type` 都改成 `mysql`
   - 指向**同一組** MySQL 資料庫設定
   - 在其中一台用 `/setlobby` 設定好座標後，另一台會在 `sync-interval-seconds`
     設定的秒數內自動同步到該筆 Lobby 資料
3. 跨伺服器傳送是透過 `BungeeCord` 舊版插件訊息頻道呼叫 Velocity 的玩家轉移功能，
   **不需要**在 Velocity 額外裝插件，但要確認 Velocity 沒有停用該相容頻道
   （`velocity.toml` 裡通常不需要特別設定，預設就相容）。

## 指令與權限

| 指令 | 說明 | 權限 |
|---|---|---|
| `/lobby [名稱]` | 傳送到指定 Lobby（省略則用 `default-lobby`） | `betterlobby.use`（預設所有人） |
| `/setlobby [名稱]` | 將目前位置設為 Lobby | `betterlobby.setlobby`（預設 OP） |
| `/betterlobby reload` | 重載 config.yml 與 messages.yml | `betterlobby.reload`（預設 OP） |

另有 `betterlobby.bypass` 權限，持有者在傳送倒數期間不受移動/受傷取消影響（預設 OP）。

## 已知限制

- SQLite 模式下 Lobby 資料僅存在本機，無法跨伺服器同步（這是設計上的取捨，非 bug）。
- 跨伺服器傳送當下不會有第二次倒數／粒子效果，玩家會直接被丟到目標伺服器上設定好的座標，
  這是為了避免玩家在切換伺服器的瞬間看到奇怪的中斷畫面。
- `teleport.delay: 0` 時，不會顯示倒數、不會播放倒數音效與粒子效果，直接傳送（符合你的需求）。
