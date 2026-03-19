package fr.dragon.admincore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.dragon.admincore.util.ConfigLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager {

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    private final JavaPlugin plugin;
    private final ConfigLoader configLoader;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private HikariDataSource dataSource;
    private DatabaseConfig currentConfig;

    public DatabaseManager(final JavaPlugin plugin, final ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
    }

    public void start() {
        this.currentConfig = DatabaseConfig.from(this.configLoader.config());
        this.dataSource = new HikariDataSource(buildConfig(this.currentConfig));
        initializeSchema();
    }

    public <T> CompletableFuture<T> query(final Function<Connection, T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection()) {
                return action.apply(connection);
            } catch (final Exception exception) {
                throw new IllegalStateException("Echec d'une operation SQL", exception);
            }
        }, this.executor);
    }

    public CompletableFuture<Void> execute(final SqlConsumer action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection()) {
                action.accept(connection);
            } catch (final Exception exception) {
                throw new IllegalStateException("Echec d'une operation SQL", exception);
            }
        }, this.executor);
    }

    public Path sqlitePath() {
        return this.plugin.getDataFolder().toPath().resolve(this.currentConfig.sqliteFile());
    }

    public ExecutorService executor() {
        return this.executor;
    }

    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
        this.executor.shutdownNow();
    }

    private HikariConfig buildConfig(final DatabaseConfig config) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AdminCorePool");
        hikariConfig.setConnectionTimeout(10_000L);
        hikariConfig.setValidationTimeout(5_000L);
        hikariConfig.setLeakDetectionThreshold(30_000L);
        hikariConfig.setMaximumPoolSize(config.useMySql() ? config.poolSize() : 1);
        if (config.useMySql()) {
            hikariConfig.setJdbcUrl(
                "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database()
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            );
            hikariConfig.setUsername(config.username());
            hikariConfig.setPassword(config.password());
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            final Path sqlitePath = this.plugin.getDataFolder().toPath().resolve(config.sqliteFile());
            try {
                Files.createDirectories(sqlitePath.getParent());
            } catch (final IOException exception) {
                throw new IllegalStateException("Impossible de creer le dossier SQLite", exception);
            }
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqlitePath);
        }
        return hikariConfig;
    }

    private void initializeSchema() {
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            if (!this.currentConfig.useMySql()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
            }

            statement.addBatch("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    last_name VARCHAR(32) NOT NULL,
                    last_ip VARCHAR(64),
                    last_client_brand VARCHAR(64),
                    last_level INTEGER NOT NULL DEFAULT 0,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL
                )
                """);
            statement.addBatch("""
                CREATE TABLE IF NOT EXISTS player_ips (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(32) NOT NULL,
                    ip VARCHAR(64) NOT NULL,
                    seen_at BIGINT NOT NULL
                )
                """);
            statement.addBatch("""
                CREATE TABLE IF NOT EXISTS sanctions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid VARCHAR(36),
                    target_name VARCHAR(32) NOT NULL,
                    actor_uuid VARCHAR(36),
                    actor_name VARCHAR(32) NOT NULL,
                    type VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT,
                    active INTEGER NOT NULL,
                    scope VARCHAR(16) NOT NULL,
                    scope_value VARCHAR(128) NOT NULL,
                    linked_target_uuid VARCHAR(36)
                )
                """);
            statement.addBatch("""
                CREATE TABLE IF NOT EXISTS player_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid VARCHAR(36),
                    target_name VARCHAR(32) NOT NULL,
                    actor_uuid VARCHAR(36),
                    actor_name VARCHAR(32) NOT NULL,
                    note VARCHAR(255) NOT NULL,
                    created_at BIGINT NOT NULL
                )
                """);
            statement.executeBatch();
            alterIgnore(statement, "ALTER TABLE players ADD COLUMN last_client_brand VARCHAR(64)");
            alterIgnore(statement, "ALTER TABLE players ADD COLUMN last_level INTEGER NOT NULL DEFAULT 0");
            alterIgnore(statement, "ALTER TABLE sanctions ADD COLUMN linked_target_uuid VARCHAR(36)");
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Impossible d'initialiser le schema SQL", exception);
            throw new IllegalStateException("Base AdminCore indisponible", exception);
        }
    }

    private void alterIgnore(final Statement statement, final String sql) {
        try {
            statement.execute(sql);
        } catch (final SQLException ignored) {
        }
    }
}
