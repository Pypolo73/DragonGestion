package fr.dragon.admincore.database;

import org.bukkit.configuration.file.FileConfiguration;

public record DatabaseConfig(
    String type,
    String sqliteFile,
    String host,
    int port,
    String database,
    String username,
    String password,
    int poolSize
) {

    public static DatabaseConfig from(final FileConfiguration config) {
        return new DatabaseConfig(
            config.getString("database.type", "SQLITE").toUpperCase(),
            config.getString("database.sqlite.file", "admincore.db"),
            config.getString("database.mysql.host", "localhost"),
            config.getInt("database.mysql.port", 3306),
            config.getString("database.mysql.database", "admincore"),
            config.getString("database.mysql.username", "root"),
            config.getString("database.mysql.password", ""),
            Math.max(2, config.getInt("database.mysql.pool-size", 10))
        );
    }

    public boolean useMySql() {
        return "MYSQL".equalsIgnoreCase(this.type);
    }
}
