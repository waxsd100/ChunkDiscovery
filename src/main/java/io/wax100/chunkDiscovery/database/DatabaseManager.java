package io.wax100.chunkDiscovery.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseManager {
    private static HikariDataSource ds;

    // MySQL用の初期化メソッド
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

    // SQLite用の初期化メソッド
    public static void initSQLite(File dataFolder) {
        HikariConfig cfg = new HikariConfig();
        File dbFile = new File(dataFolder, "chunks.db");
        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        cfg.setMaximumPoolSize(1);
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

            // プレイヤーテーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "  player_id TEXT NOT NULL PRIMARY KEY," +
                            "  total_chunks INTEGER NOT NULL DEFAULT 0," +
                            "  last_update TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            // グローバルチャンクテーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS global_chunks (" +
                            "  world TEXT NOT NULL," +
                            "  chunk_x INTEGER NOT NULL," +
                            "  chunk_z INTEGER NOT NULL," +
                            "  discovered_by TEXT NOT NULL," +
                            "  discovered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (world, chunk_x, chunk_z)" +
                            ")"
            );

            // プレイヤーチャンクテーブル
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS player_chunks (" +
                            "  player_id TEXT NOT NULL," +
                            "  world TEXT NOT NULL," +
                            "  chunk_x INTEGER NOT NULL," +
                            "  chunk_z INTEGER NOT NULL," +
                            "  discovered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (player_id, world, chunk_x, chunk_z)" +
                            ")"
            );

            // ワールドボーダーテーブル（新規追加）
            stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS world_borders (" +
                            "  world_name TEXT NOT NULL PRIMARY KEY," +
                            "  border_size REAL NOT NULL," +
                            "  total_chunks_discovered INTEGER NOT NULL DEFAULT 0," +
                            "  last_update TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            // インデックス作成
            stmt.addBatch("CREATE INDEX IF NOT EXISTS idx_player_total ON player_chunks(player_id)");

            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables for ChunkDiscovery", e);
        }
    }
}