package net.xtte.betterlobby.storage;

import net.xtte.betterlobby.model.LobbyLocation;
import net.xtte.betterlobby.model.TeleportPoint;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite 儲存實作。僅本機儲存，不支援跨伺服器同步。
 * SQLite 對同時寫入的支援有限，因此所有操作皆透過單一連線並加上同步鎖。
 */
public class SQLiteStorage implements StorageManager {

    private final File dataFolder;
    private Connection connection;
    private final Object lock = new Object();

    public SQLiteStorage(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void init() throws Exception {
        // JDBC4 驅動會透過 META-INF/services 自動註冊，不需手動 Class.forName。
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "betterlobby.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS betterlobby_lobbies (
                        name TEXT PRIMARY KEY,
                        server TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS betterlobby_pending (
                        uuid TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
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
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void saveDefaultLobby(String server, TeleportPoint point) throws Exception {
        synchronized (lock) {
            String sql = """
                    INSERT INTO betterlobby_lobbies (name, server, world, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(name) DO UPDATE SET
                        server = excluded.server,
                        world = excluded.world,
                        x = excluded.x,
                        y = excluded.y,
                        z = excluded.z,
                        yaw = excluded.yaw,
                        pitch = excluded.pitch
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, "default");
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
    }

    @Override
    public Optional<LobbyLocation> getDefaultLobby() throws Exception {
        synchronized (lock) {
            String sql = "SELECT * FROM betterlobby_lobbies WHERE name = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, "default");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public void addPendingTeleport(UUID uuid, TeleportPoint point) throws Exception {
        synchronized (lock) {
            String sql = """
                    INSERT INTO betterlobby_pending (uuid, world, x, y, z, yaw, pitch, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        world = excluded.world,
                        x = excluded.x,
                        y = excluded.y,
                        z = excluded.z,
                        yaw = excluded.yaw,
                        pitch = excluded.pitch,
                        created_at = excluded.created_at
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
    }

    @Override
    public Optional<TeleportPoint> pollPendingTeleport(UUID uuid) throws Exception {
        synchronized (lock) {
            String select = "SELECT * FROM betterlobby_pending WHERE uuid = ?";
            TeleportPoint point = null;
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
            return Optional.ofNullable(point);
        }
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
