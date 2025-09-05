package fr.ax_dev.universejobs.storage.database;

public enum DatabaseType {
    MYSQL("mysql", "jdbc:mysql://", 3306),
    SQLITE("sqlite", "jdbc:sqlite:", 0);

    private final String name;
    private final String protocol;
    private final int defaultPort;

    DatabaseType(String name, String protocol, int defaultPort) {
        this.name = name;
        this.protocol = protocol;
        this.defaultPort = defaultPort;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public static DatabaseType fromString(String type) {
        for (DatabaseType dbType : values()) {
            if (dbType.name.equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        return SQLITE;
    }
}