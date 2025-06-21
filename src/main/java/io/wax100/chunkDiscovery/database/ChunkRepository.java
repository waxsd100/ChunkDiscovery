package io.wax100.chunkDiscovery.database;

import org.bukkit.Chunk;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChunkRepository {
    private final DataSource ds;

    public ChunkRepository(DataSource ds) {
        this.ds = ds;
    }

    /**
     * グローバルチャンクテーブルに新規チャンクを保存（重複時は無視）
     * @param chunk 発見されたチャンク
     * @param playerId 発見者のUUID
     * @return 新規追加された場合true、既に存在していた場合false
     */
    public boolean saveIfAbsent(Chunk chunk, String playerId) {
        String world = chunk.getWorld().getName();
        int x = chunk.getX(), z = chunk.getZ();
        String sql = "INSERT IGNORE INTO global_chunks(world, chunk_x, chunk_z, discovered_by) VALUES(?,?,?,?)";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, z);
            ps.setString(4, playerId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("グローバルチャンク保存中にエラーが発生しました", e);
        }
    }

    /**
     * サーバー全体で発見済みのチャンク総数を取得
     * @return 発見済みチャンク総数
     */
    public int getTotalDiscoveredChunks() {
        String sql = "SELECT COUNT(*) as total FROM global_chunks";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("グローバル統計取得中にエラーが発生しました", e);
        }
    }

    /**
     * 特定のワールドで発見済みのチャンク総数を取得
     * @param worldName ワールド名
     * @return 該当ワールドの発見済みチャンク総数
     */
    public int getTotalDiscoveredChunksByWorld(String worldName) {
        String sql = "SELECT COUNT(*) as total FROM global_chunks WHERE world = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ワールド別統計取得中にエラーが発生しました", e);
        }
    }

    /**
     * 特定のチャンクが既に発見されているかチェック
     * @param chunk チェック対象のチャンク
     * @return 発見済みならtrue
     */
    public boolean isChunkDiscovered(Chunk chunk) {
        String sql = "SELECT 1 FROM global_chunks WHERE world = ? AND chunk_x = ? AND chunk_z = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("チャンク発見状況確認中にエラーが発生しました", e);
        }
    }

    /**
     * 特定のプレイヤーが発見したチャンク数を取得
     * @param playerId プレイヤーのUUID
     * @return 該当プレイヤーが発見したチャンク数
     */
    public int getDiscoveredChunksByPlayer(String playerId) {
        String sql = "SELECT COUNT(*) as total FROM global_chunks WHERE discovered_by = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("プレイヤー別統計取得中にエラーが発生しました", e);
        }
    }

    /**
     * 最近発見されたチャンクのリストを取得
     * @param limit 取得する件数
     * @return 最近発見されたチャンクの情報
     */
    public java.util.List<io.wax100.chunkDiscovery.model.DiscoveredChunk> getRecentDiscoveries(int limit) {
        String sql = "SELECT world, chunk_x, chunk_z, discovered_by, discovered_at " +
                "FROM global_chunks ORDER BY discovered_at DESC LIMIT ?";

        java.util.List<io.wax100.chunkDiscovery.model.DiscoveredChunk> discoveries = new java.util.ArrayList<>();

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    discoveries.add(new io.wax100.chunkDiscovery.model.DiscoveredChunk(
                            rs.getString("world"),
                            rs.getInt("chunk_x"),
                            rs.getInt("chunk_z"),
                            rs.getString("discovered_by"),
                            rs.getTimestamp("discovered_at").toLocalDateTime()
                    ));
                }
            }
            return discoveries;
        } catch (SQLException e) {
            throw new RuntimeException("最近の発見履歴取得中にエラーが発生しました", e);
        }
    }
}