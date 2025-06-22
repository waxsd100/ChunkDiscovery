package io.wax100.chunkDiscovery.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseManager {
    private static HikariDataSource ds;

    public static void init(String host, int port, String db, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=UTC");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(5000);
        cfg.setPoolName("ChunkDiscoveryPool");
        ds = new HikariDataSource(cfg);

        createTables();
    }

    public static HikariDataSource getDataSource() {
        if (ds == null) {
            throw new IllegalStateException("DatabaseManager is not initialized!");
        }
        return ds;
    }

    public static void shutdown() {
        if (ds != null) {
            ds.close();
        }
    }

    private static void createTables() {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1) プレイヤー情報テーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "  player_id CHAR(36) NOT NULL," +
                            "  total_chunks INT NOT NULL DEFAULT 0," +
                            "  last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (player_id)" +
                            ");"
            );

            // 2) ワールド境界テーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS world_borders (" +
                            "  world_name VARCHAR(64) NOT NULL," +
                            "  border_size DOUBLE NOT NULL," +
                            "  total_chunks_discovered INT NOT NULL DEFAULT 0," +
                            "  last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (world_name)" +
                            ");"
            );

            // 3) 全体チャンク発見記録テーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS global_chunks (" +
                            "  world VARCHAR(64) NOT NULL," +
                            "  chunk_x INT NOT NULL," +
                            "  chunk_z INT NOT NULL," +
                            "  discovered_by CHAR(36) NOT NULL," +
                            "  discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (world, chunk_x, chunk_z)," +
                            "  CONSTRAINT fk_global_chunks_player FOREIGN KEY (discovered_by) REFERENCES players (player_id) ON DELETE RESTRICT ON UPDATE CASCADE," +
                            "  CONSTRAINT fk_global_chunks_world FOREIGN KEY (world) REFERENCES world_borders (world_name) ON DELETE CASCADE ON UPDATE CASCADE" +
                            ");"
            );

            // 4) プレイヤー別チャンク発見記録テーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS player_chunks (" +
                            "  player_id CHAR(36) NOT NULL," +
                            "  world VARCHAR(64) NOT NULL," +
                            "  chunk_x INT NOT NULL," +
                            "  chunk_z INT NOT NULL," +
                            "  discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (player_id, world, chunk_x, chunk_z)," +
                            "  INDEX idx_player_total (player_id)," +
                            "  CONSTRAINT fk_player_chunks_player FOREIGN KEY (player_id) REFERENCES players (player_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                            "  CONSTRAINT fk_player_chunks_global FOREIGN KEY (world, chunk_x, chunk_z) REFERENCES global_chunks (world, chunk_x, chunk_z) ON DELETE CASCADE ON UPDATE CASCADE" +
                            ");"
            );

            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables for ChunkDiscovery", e);
        }
    }
}