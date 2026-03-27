package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeleportDatabase {
    private final AdminCorePlugin plugin;
    private final String dbPath;
    private Connection connection;

    public TeleportDatabase(AdminCorePlugin plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "teleportation");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dbPath = new File(dataFolder, "teleportation.db").getAbsolutePath();
        initialize();
    }

    private void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to teleport database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS homes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    is_public INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    UNIQUE(owner_uuid, name)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS warps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    created_at TEXT NOT NULL,
                    description TEXT DEFAULT ''
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS spawn (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS teleport_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_uuid TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS back_positions (
                    player_uuid TEXT PRIMARY KEY,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    reason TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);
        }
    }

    // Home methods
    public void saveHome(TeleportData home) {
        String sql = """
            INSERT OR REPLACE INTO homes (owner_uuid, name, world, x, y, z, yaw, pitch, is_public, created_at, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, home.owner().toString());
            stmt.setString(2, home.name());
            stmt.setString(3, home.world());
            stmt.setDouble(4, home.x());
            stmt.setDouble(5, home.y());
            stmt.setDouble(6, home.z());
            stmt.setDouble(7, home.yaw());
            stmt.setDouble(8, home.pitch());
            stmt.setInt(9, home.isPublic() ? 1 : 0);
            stmt.setString(10, home.createdAt().toString());
            stmt.setString(11, home.description());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save home: " + e.getMessage());
        }
    }

    public List<TeleportData> getHomes(UUID owner) {
        List<TeleportData> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE owner_uuid = ? ORDER BY name";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, owner.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                homes.add(resultToHome(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get homes: " + e.getMessage());
        }
        return homes;
    }

    public TeleportData getHome(UUID owner, String name) {
        String sql = "SELECT * FROM homes WHERE owner_uuid = ? AND name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, owner.toString());
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultToHome(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get home: " + e.getMessage());
        }
        return null;
    }

    public void deleteHome(UUID owner, String name) {
        String sql = "DELETE FROM homes WHERE owner_uuid = ? AND name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, owner.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete home: " + e.getMessage());
        }
    }

    public int getHomeCount(UUID owner) {
        String sql = "SELECT COUNT(*) FROM homes WHERE owner_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, owner.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to count homes: " + e.getMessage());
        }
        return 0;
    }

    public List<TeleportData> getPublicHomes() {
        List<TeleportData> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE is_public = 1 ORDER BY name";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                homes.add(resultToHome(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get public homes: " + e.getMessage());
        }
        return homes;
    }

    public void setHomePublic(UUID owner, String name, boolean isPublic) {
        String sql = "UPDATE homes SET is_public = ? WHERE owner_uuid = ? AND name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, isPublic ? 1 : 0);
            stmt.setString(2, owner.toString());
            stmt.setString(3, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update home public status: " + e.getMessage());
        }
    }

    private TeleportData resultToHome(ResultSet rs) throws SQLException {
        return new TeleportData(
            rs.getLong("id"),
            UUID.fromString(rs.getString("owner_uuid")),
            rs.getString("name"),
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch"),
            rs.getInt("is_public") == 1,
            Instant.parse(rs.getString("created_at")),
            rs.getString("description")
        );
    }

    // Warp methods
    public void saveWarp(TeleportData warp) {
        String sql = """
            INSERT OR REPLACE INTO warps (name, world, x, y, z, yaw, pitch, created_at, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, warp.name());
            stmt.setString(2, warp.world());
            stmt.setDouble(3, warp.x());
            stmt.setDouble(4, warp.y());
            stmt.setDouble(5, warp.z());
            stmt.setDouble(6, warp.yaw());
            stmt.setDouble(7, warp.pitch());
            stmt.setString(8, warp.createdAt().toString());
            stmt.setString(9, warp.description());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save warp: " + e.getMessage());
        }
    }

    public List<TeleportData> getWarps() {
        List<TeleportData> warps = new ArrayList<>();
        String sql = "SELECT * FROM warps ORDER BY name";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                warps.add(resultToWarp(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warps: " + e.getMessage());
        }
        return warps;
    }

    public TeleportData getWarp(String name) {
        String sql = "SELECT * FROM warps WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultToWarp(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warp: " + e.getMessage());
        }
        return null;
    }

    public void deleteWarp(String name) {
        String sql = "DELETE FROM warps WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete warp: " + e.getMessage());
        }
    }

    private TeleportData resultToWarp(ResultSet rs) throws SQLException {
        return new TeleportData(
            rs.getLong("id"),
            null,
            rs.getString("name"),
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch"),
            false,
            Instant.parse(rs.getString("created_at")),
            rs.getString("description")
        );
    }

    // Spawn methods
    public void saveSpawn(Location loc) {
        String sql = """
            INSERT OR REPLACE INTO spawn (id, world, x, y, z, yaw, pitch)
            VALUES (1, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setDouble(2, loc.getX());
            stmt.setDouble(3, loc.getY());
            stmt.setDouble(4, loc.getZ());
            stmt.setDouble(5, loc.getYaw());
            stmt.setDouble(6, loc.getPitch());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save spawn: " + e.getMessage());
        }
    }

    public Location getSpawn() {
        String sql = "SELECT * FROM spawn WHERE id = 1";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                org.bukkit.World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world == null) {
                    world = plugin.getServer().getWorlds().get(0);
                }
                return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get spawn: " + e.getMessage());
        }
        return null;
    }

    // TPA Request methods
    public void saveTpaRequest(UUID sender, UUID target, String type, long timeoutSeconds) {
        String sql = """
            INSERT INTO teleport_requests (sender_uuid, target_uuid, type, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Instant now = Instant.now();
            stmt.setString(1, sender.toString());
            stmt.setString(2, target.toString());
            stmt.setString(3, type);
            stmt.setString(4, now.toString());
            stmt.setString(5, now.plusSeconds(timeoutSeconds).toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save TPA request: " + e.getMessage());
        }
    }

    public TeleportRequest getTpaRequest(UUID target) {
        String sql = "SELECT * FROM teleport_requests WHERE target_uuid = ? AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, target.toString());
            stmt.setString(2, Instant.now().toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new TeleportRequest(
                    rs.getLong("id"),
                    UUID.fromString(rs.getString("sender_uuid")),
                    UUID.fromString(rs.getString("target_uuid")),
                    rs.getString("type"),
                    Instant.parse(rs.getString("created_at")),
                    Instant.parse(rs.getString("expires_at"))
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get TPA request: " + e.getMessage());
        }
        return null;
    }

    public void deleteTpaRequest(UUID target) {
        String sql = "DELETE FROM teleport_requests WHERE target_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, target.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete TPA request: " + e.getMessage());
        }
    }

    public void deleteExpiredRequests() {
        String sql = "DELETE FROM teleport_requests WHERE expires_at <= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, Instant.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete expired requests: " + e.getMessage());
        }
    }

    // Back position methods
    public void saveBackPosition(Player player, Location loc, String reason) {
        String sql = """
            INSERT OR REPLACE INTO back_positions (player_uuid, world, x, y, z, yaw, pitch, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, loc.getWorld().getName());
            stmt.setDouble(3, loc.getX());
            stmt.setDouble(4, loc.getY());
            stmt.setDouble(5, loc.getZ());
            stmt.setDouble(6, loc.getYaw());
            stmt.setDouble(7, loc.getPitch());
            stmt.setString(8, reason);
            stmt.setString(9, Instant.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save back position: " + e.getMessage());
        }
    }

    public Location getBackPosition(UUID player) {
        String sql = "SELECT * FROM back_positions WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                org.bukkit.World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world == null) {
                    world = plugin.getServer().getWorlds().get(0);
                }
                return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get back position: " + e.getMessage());
        }
        return null;
    }

    public void deleteBackPosition(UUID player) {
        String sql = "DELETE FROM back_positions WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete back position: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }

    public record TeleportRequest(
        long id,
        UUID sender,
        UUID target,
        String type,
        Instant createdAt,
        Instant expiresAt
    ) {}
}
