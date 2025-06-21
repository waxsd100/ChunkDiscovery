package io.wax100.chunkDiscovery.database;

import org.bukkit.Chunk;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ChunkRepository {
    private final DataSource ds;

    public ChunkRepository(DataSource ds) {
        this.ds = ds;
    }

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
            throw new RuntimeException(e);
        }
    }
}
