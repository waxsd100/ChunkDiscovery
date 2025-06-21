package io.wax100.chunkDiscovery.database;

import io.wax100.chunkDiscovery.model.PlayerData;
import org.bukkit.Chunk;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerRepository {
    private final DataSource ds;

    public PlayerRepository(DataSource ds) {
        this.ds = ds;
    }

    public boolean saveIfAbsentChunk(String playerId, Chunk chunk) {
        String world = chunk.getWorld().getName();
        int x = chunk.getX(), z = chunk.getZ();
        String sql = "INSERT IGNORE INTO player_chunks(player_id, world, chunk_x, chunk_z) VALUES(?,?,?,?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setString(2, world);
            ps.setInt(3, x);
            ps.setInt(4, z);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public PlayerData incrementTotalChunks(String playerId) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO players(player_id) VALUES(?)")) {
                ps.setString(1, playerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE players SET total_chunks = total_chunks + 1 WHERE player_id = ?")) {
                ps.setString(1, playerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT total_chunks FROM players WHERE player_id = ?")) {
                ps.setString(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt("total_chunks");
                        conn.commit();
                        return new PlayerData(playerId, total);
                    } else {
                        conn.rollback();
                        throw new RuntimeException("Player not found after increment");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTotalChunks(String playerId) {
        String sql = "SELECT total_chunks FROM players WHERE player_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total_chunks") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PlayerData> getTopPlayers(int limit) {
        String sql = "SELECT player_id, total_chunks FROM players ORDER BY total_chunks DESC LIMIT ?";
        List<PlayerData> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerData(rs.getString("player_id"), rs.getInt("total_chunks")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasDiscoveredChunk(String playerId, Chunk chunk) {
        String sql = "SELECT 1 FROM player_chunks WHERE player_id = ? AND world = ? AND chunk_x = ? AND chunk_z = ? LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setString(2, chunk.getWorld().getName());
            ps.setInt(3, chunk.getX());
            ps.setInt(4, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
