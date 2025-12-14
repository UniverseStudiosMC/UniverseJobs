package fr.ax_dev.universejobs.storage.database;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public class DatabaseConfig {

    private final boolean enabled;
    private final DatabaseType type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String prefix;

    private final int minConnections;
    private final int maxConnections;
    private final long connectionTimeoutMs;
    private final long validationIntervalMs;

    public DatabaseConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", false);

        String typeString = config.getString("type", "sqlite");
        this.type = DatabaseType.fromString(typeString);

        ConfigurationSection databaseConfig = Objects.requireNonNullElse(config.getConfigurationSection(typeString), config.createSection(typeString));

        this.host = databaseConfig.getString("host", "localhost");
        this.port = databaseConfig.getInt("port", type.getDefaultPort());
        this.database = databaseConfig.getString("database", "universejobs");
        this.username = databaseConfig.getString("username", "root");
        this.password = databaseConfig.getString("password", "");

        this.prefix = config.getString("prefix", "universejobs_");

        ConfigurationSection poolConfig = config.getConfigurationSection("pool");
        if (poolConfig != null) {
            this.minConnections = poolConfig.getInt("min-connections", 2);
            this.maxConnections = poolConfig.getInt("max-connections", 10);
            this.connectionTimeoutMs = poolConfig.getLong("connection-timeout-ms", 30000);
            this.validationIntervalMs = poolConfig.getLong("validation-interval-ms", 300000);
        } else {
            this.minConnections = 2;
            this.maxConnections = 10;
            this.connectionTimeoutMs = 30000;
            this.validationIntervalMs = 300000;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DatabaseType getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public long getValidationIntervalMs() {
        return validationIntervalMs;
    }

    public String buildJdbcUrl(String dataFolder) {
        switch (type) {
            case MYSQL:
                return String.format("%s%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    type.getProtocol(), host, port, database);
            case SQLITE:
            default:
                return type.getProtocol() + dataFolder + "/data.db";
        }
    }
}
