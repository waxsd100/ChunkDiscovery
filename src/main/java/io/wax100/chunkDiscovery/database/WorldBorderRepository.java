package io.wax100.chunkDiscovery.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ワールドボーダーサイズをデータベースで管理するリポジトリクラス（MySQL専用）
 */
public class WorldBorderRepository {
    private final DataSource ds;

    public WorldBorderRepository(DataSource ds) {
        this.ds = ds;
    }

    /**
     * ワールドのボーダーサイズを保存・更新
     * @param worldName ワールド名
     * @param borderSize ボーダーサイズ
     * @param totalChunks 発見済みチャンク総数
     */
    public void saveBorderSize(String worldName, double borderSize, int totalChunks) {
        String sql = "INSERT INTO world_borders(world_name, border_size, total_chunks_discovered) " +
                "VALUES(?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "border_size = VALUES(border_size), " +
                "total_chunks_discovered = VALUES(total_chunks_discovered), " +
                "last_update = CURRENT_TIMESTAMP";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.setDouble(2, borderSize);
            ps.setInt(3, totalChunks);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ワールドボーダーサイズ保存中にエラーが発生しました", e);
        }
    }

    /**
     * ワールドのボーダーサイズを取得
     * @param worldName ワールド名
     * @return ボーダーサイズ（存在しない場合は null）
     */
    public Double getBorderSize(String worldName) {
        String sql = "SELECT border_size FROM world_borders WHERE world_name = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("border_size") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ワールドボーダーサイズ取得中にエラーが発生しました", e);
        }
    }

    /**
     * ワールドの発見済みチャンク総数を取得
     * @param worldName ワールド名
     * @return 発見済みチャンク総数
     */
    public int getTotalChunksDiscovered(String worldName) {
        String sql = "SELECT total_chunks_discovered FROM world_borders WHERE world_name = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total_chunks_discovered") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ワールド別チャンク数取得中にエラーが発生しました", e);
        }
    }

    /**
     * 全ワールドのボーダー情報を取得
     * @return ワールド名 -> ボーダーサイズ のマップ
     */
    public Map<String, Double> getAllBorderSizes() {
        String sql = "SELECT world_name, border_size FROM world_borders";
        Map<String, Double> borders = new HashMap<>();

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                borders.put(rs.getString("world_name"), rs.getDouble("border_size"));
            }
            return borders;
        } catch (SQLException e) {
            throw new RuntimeException("全ワールドボーダー情報取得中にエラーが発生しました", e);
        }
    }

    /**
     * ワールドの初期ボーダーサイズを設定（存在しない場合のみ）
     * @param worldName ワールド名
     * @param initialSize 初期サイズ
     */
    public void initializeBorderIfAbsent(String worldName, double initialSize) {
        String sql = "INSERT IGNORE INTO world_borders(world_name, border_size, total_chunks_discovered) " +
                "VALUES(?, ?, 0)";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.setDouble(2, initialSize);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ワールドボーダー初期化中にエラーが発生しました", e);
        }
    }

    /**
     * 特定ワールドのボーダー情報を削除
     * @param worldName ワールド名
     */
    public void deleteBorderInfo(String worldName) {
        String sql = "DELETE FROM world_borders WHERE world_name = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ワールドボーダー情報削除中にエラーが発生しました", e);
        }
    }
}