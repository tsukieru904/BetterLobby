# BetterLobby

BetterLobby 是一個支援跨分流同步的 Lobby 傳送插件，適用於 Purpur 26.1.2 與 Velocity 代理架構。

## 主要功能

- **單一 Lobby 模式**：只支援一個預設 Lobby，不需要指定名稱。
- `/setlobby`：將當前所在位置設為唯一 Lobby。
- `/lobby`：傳送到該預設 Lobby。
- MySQL 模式可讓多台伺服器共用同一組 Lobby 設定。
- 支援跨伺服器傳送與待處理傳送紀錄。

## 要求

- Purpur 26.1.2
- Java 25
- Velocity 代理，伺服器名稱必須與 `velocity.toml` 的 `[servers]` 名稱一致

## 快速安裝

1. 編譯插件：

```bash
mvn clean package
```

2. 取得 jar：

`target/BetterLobby-1.0.0.jar`

3. 將 jar 放到每個後端伺服器的 `plugins/` 資料夾。

4. 設定每台伺服器的 `config.yml`：
   - `server-name` 改為該伺服器在 Velocity 的名稱
   - 若要共用 Lobby，將 `storage.type` 設為 `mysql`，並使用相同的 MySQL 資料庫設定

## 功能說明

### 單一 Lobby

這個插件不再支援 `/lobby 名稱` 或 `/setlobby 名稱`。
目前的使用方式是：

- `/setlobby`：設定當前位置為伺服器的 Lobby
- `/lobby`：傳送到目前設定好的 Lobby

也就是一個伺服器只會有一個 Lobby 設定，沒有多個 Lobby 名稱管理。

### MySQL 共享 Lobby

如果兩台或多台伺服器使用相同 MySQL 資料庫：

- 其中一台使用 `/setlobby` 設定位置
- 其他伺服器會依照 `storage.mysql.sync-interval-seconds` 週期同步最新 Lobby
- 這樣就可以讓分流伺服器共用同一個 Lobby

### SQLite

SQLite 模式只儲存本機資料，**無法跨伺服器同步**。
如果你需要多台伺服器共用 Lobby，請使用 MySQL。

## 指令與權限

| 指令 | 說明 | 權限 |
|---|---|---|
| `/lobby` | 傳送到已設定的 Lobby | `betterlobby.use`（預設 true） |
| `/setlobby` | 設定目前位置為 Lobby | `betterlobby.setlobby`（預設 op） |
| `/betterlobby reload` | 重新載入設定檔與語言檔 | `betterlobby.reload`（預設 op） |

### 進階權限

- `betterlobby.bypass`：持有者在倒數期間不會因為移動或受到攻擊而取消傳送，預設為 `false`。

## 主要設定項目

```yaml
server-name: "lobby-1"

storage:
  type: sqlite
  mysql:
    host: 127.0.0.1
    port: 3306
    database: betterlobby
    username: root
    password: ""
    useSSL: false
    pool-size: 10
    sync-interval-seconds: 30

teleport:
  delay: 5
  cancel-on-move: true
  cancel-on-damage: true
  invulnerable-during-teleport: false
  actionbar-countdown: true
  countdown-sound:
    enabled: true
    sound: "UI_BUTTON_CLICK"
    volume: 1.0
    pitch: 1.0
  countdown-particle:
    enabled: true
    particle: "PORTAL"
    count: 15
    offset-x: 0.5
    offset-y: 1.0
    offset-z: 0.5
    speed: 0.02
  teleport-sound:
    enabled: true
    sound: "ENTITY_ENDERMAN_TELEPORT"
    volume: 1.0
    pitch: 1.0

default-lobby: "main"
```

### 傳送行為

- `delay: 0`：立即傳送，無倒數、無音效、無粒子
- `cancel-on-move: true`：倒數期間移動會取消傳送
- `cancel-on-damage: true`：倒數期間受到攻擊會取消傳送
- `invulnerable-during-teleport: true`：倒數期間玩家無敵，不會被攻擊取消

## 其他說明

- 插件透過內建的 JDBC driver 來支援 SQLite / MySQL，因此 jar 檔較大。
- 目前支援的跨伺服器傳送機制，是從伺服器端透過代理連線玩家，無需在 Velocity 安裝額外插件。
- 如果你只需要單機伺服器，建議使用 `sqlite`。
- 如果要多伺服器共用 Lobby，請使用 `mysql` 並確保所有伺服器指向相同資料庫。
