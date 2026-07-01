package net.xtte.betterlobby.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.model.TeleportPoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL 儲存實作，使用 HikariCP 連線池。
 * 多台伺服器共用同一組資料庫設定時，Lobby 資料即可跨伺服器同步。
 */
public class MySQLStorage implements StorageManager {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final int poolSize;

    private HikariDataSource dataSource;

    public MySQLStorage(String host, int port, String database, String username,
                         String password, boolean useSSL, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.poolSize = poolSize;
    }

    @Override
    public void init() throws Exception {
        // JDBC4 驅動會透過 META-INF/services 自動註冊，不需手動 Class.forName。
        // (pom.xml 已設定 ServicesResourceTransformer，確保重新定位後仍能正確自動載入)
        HikariConfig config = new HikariConfig();
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&autoReconnect=true&characterEncoding=utf8";
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("BetterLobby-Pool");

        dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS betterlobby_lobbies (
                        name VARCHAR(64) PRIMARY KEY,
                        server VARCHAR(64) NOT NULL,
                        world VARCHAR(64) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS betterlobby_pending (
                        uuid VARCHAR(36) PRIMARY KEY,
                        world VARCHAR(64) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL,
                        created_at BIGINT NOT NULL
                    )
                    """);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void saveLobby(String name, String server, TeleportPoint point) throws Exception {
        String sql = """
                INSERT INTO betterlobby_lobbies (name, server, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    server = VALUES(server),
                    world = VALUES(world),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    yaw = VALUES(yaw),
                    pitch = VALUES(pitch)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, server);
            ps.setString(3, point.world());
            ps.setDouble(4, point.x());
            ps.setDouble(5, point.y());
            ps.setDouble(6, point.z());
            ps.setFloat(7, point.yaw());
            ps.setFloat(8, point.pitch());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<LobbyLocation> getLobby(String name) throws Exception {
        String sql = "SELECT * FROM betterlobby_lobbies WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<LobbyLocation> getAllLobbies() throws Exception {
        List<LobbyLocation> list = new ArrayList<>();
        String sql = "SELECT * FROM betterlobby_lobbies";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public void addPendingTeleport(UUID uuid, TeleportPoint point) throws Exception {
        String sql = """
                INSERT INTO betterlobby_pending (uuid, world, x, y, z, yaw, pitch, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    world = VALUES(world),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    yaw = VALUES(yaw),
                    pitch = VALUES(pitch),
                    created_at = VALUES(created_at)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, point.world());
            ps.setDouble(3, point.x());
            ps.setDouble(4, point.y());
            ps.setDouble(5, point.z());
            ps.setFloat(6, point.yaw());
            ps.setFloat(7, point.pitch());
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<TeleportPoint> pollPendingTeleport(UUID uuid) throws Exception {
        String select = "SELECT * FROM betterlobby_pending WHERE uuid = ?";
        TeleportPoint point = null;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(select)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        point = new TeleportPoint(
                                rs.getString("world"),
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );
                    }
                }
            }
            if (point != null) {
                try (PreparedStatement del = connection.prepareStatement("DELETE FROM betterlobby_pending WHERE uuid = ?")) {
                    del.setString(1, uuid.toString());
                    del.executeUpdate();
                }
            }
        }
        return Optional.ofNullable(point);
    }

    private LobbyLocation mapRow(ResultSet rs) throws Exception {
        String name = rs.getString("name");
        String server = rs.getString("server");
        TeleportPoint point = new TeleportPoint(
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
        return new LobbyLocation(name, server, point);
    }
}
