package io.wax100.chunkDiscovery.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.wax100.chunkDiscovery.exception.DatabaseException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseManager {
    private static volatile HikariDataSource ds;
    private static final Object lock = new Object();

    public static void init(String host, int port, String db, String user, String pass) throws DatabaseException {
        if (ds != null) {
            throw new IllegalStateException("DatabaseManager is already initialized!");
        }
        
        synchronized (lock) {
            if (ds == null) {
                try {
                    HikariConfig cfg = createHikariConfig(host, port, db, user, pass);
                    ds = new HikariDataSource(cfg);
                    createTables();
                } catch (Exception e) {
                    if (ds != null) {
                        ds.close();
                        ds = null;
                    }
                    throw new DatabaseException("Failed to initialize database connection", e);
                }
            }
        }
    }

    private static HikariConfig createHikariConfig(String host, int port, String db, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&autoReconnect=true", 
            host, port, db));
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("ChunkDiscoveryPool");
        
        // コネクションプールの健全性チェック
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return config;
    }

    public static HikariDataSource getDataSource() {
        if (ds == null) {
            throw new IllegalStateException("DatabaseManager is not initialized!");
        }
        return ds;
    }

    public static boolean isInitialized() {
        return ds != null && !ds.isClosed();
    }

    public static void shutdown() {
        synchronized (lock) {
            if (ds != null && !ds.isClosed()) {
                ds.close();
                ds = null;
            }
        }
    }

    private static void createTables() throws SQLException {
        final String[] tableCreationQueries = {
            """
            CREATE TABLE IF NOT EXISTS players (
                player_id CHAR(36) NOT NULL PRIMARY KEY,
                total_chunks INT NOT NULL DEFAULT 0,
                last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_total_chunks (total_chunks DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            """
            CREATE TABLE IF NOT EXISTS global_chunks (
                world VARCHAR(64) NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                discovered_by CHAR(36) NOT NULL,
                discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (world, chunk_x, chunk_z),
                INDEX idx_discovered_by (discovered_by),
                INDEX idx_discovered_at (discovered_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            """
            CREATE TABLE IF NOT EXISTS player_chunks (
                player_id CHAR(36) NOT NULL,
                world VARCHAR(64) NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_id, world, chunk_x, chunk_z),
                INDEX idx_player_world (player_id, world),
                INDEX idx_discovered_at (discovered_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            """
            CREATE TABLE IF NOT EXISTS world_borders (
                world_name VARCHAR(64) NOT NULL PRIMARY KEY,
                border_size DOUBLE NOT NULL,
                total_chunks_discovered INT NOT NULL DEFAULT 0,
                last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            
            conn.setAutoCommit(false);
            
            for (String query : tableCreationQueries) {
                stmt.addBatch(query);
            }
            
            stmt.executeBatch();
            conn.commit();
            
        } catch (SQLException e) {
            throw new SQLException("Failed to create tables for ChunkDiscovery", e);
        }
    }
}