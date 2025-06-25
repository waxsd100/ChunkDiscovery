package io.wax100.chunkDiscovery.database;

import io.wax100.chunkDiscovery.exception.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "test.database.enabled", matches = "true")
class DatabaseManagerTest {

    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 3306;
    private static final String TEST_DATABASE = "test_chunk_discovery";
    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";

    @BeforeEach
    void setUp() throws DatabaseException {
        DatabaseManager.init(
                TEST_HOST,
                TEST_PORT,
                TEST_DATABASE,
                TEST_USERNAME,
                TEST_PASSWORD
        );
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    @Test
    void testDatabaseInitialization() {
        assertTrue(DatabaseManager.isInitialized());
        assertNotNull(DatabaseManager.getDataSource());
    }

    @Test
    void testTablesAreCreated() throws SQLException {
        String[] expectedTables = {"players", "global_chunks", "player_chunks", "world_borders"};
        
        try (Connection conn = DatabaseManager.getDataSource().getConnection()) {
            for (String tableName : expectedTables) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
                    stmt.setString(1, TEST_DATABASE);
                    stmt.setString(2, tableName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1), "Table " + tableName + " should exist");
                    }
                }
            }
        }
    }

    @Test
    void testPlayersTableStructure() throws SQLException {
        try (Connection conn = DatabaseManager.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DESCRIBE players")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                boolean hasPlayerId = false;
                boolean hasTotalChunks = false;
                boolean hasLastUpdate = false;
                
                while (rs.next()) {
                    String field = rs.getString("Field");
                    switch (field) {
                        case "player_id":
                            hasPlayerId = true;
                            assertEquals("char(36)", rs.getString("Type"));
                            assertEquals("PRI", rs.getString("Key"));
                            break;
                        case "total_chunks":
                            hasTotalChunks = true;
                            assertEquals("int", rs.getString("Type"));
                            break;
                        case "last_update":
                            hasLastUpdate = true;
                            assertTrue(rs.getString("Type").startsWith("datetime"));
                            break;
                    }
                }
                
                assertTrue(hasPlayerId, "players table should have player_id column");
                assertTrue(hasTotalChunks, "players table should have total_chunks column");
                assertTrue(hasLastUpdate, "players table should have last_update column");
            }
        }
    }

    @Test
    void testPlayerDataOperations() throws SQLException {
        String testPlayerId = "550e8400-e29b-41d4-a716-446655440000";
        int totalChunks = 42;
        
        try (Connection conn = DatabaseManager.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO players (player_id, total_chunks) VALUES (?, ?)")) {
                stmt.setString(1, testPlayerId);
                stmt.setInt(2, totalChunks);
                assertEquals(1, stmt.executeUpdate());
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT total_chunks FROM players WHERE player_id = ?")) {
                stmt.setString(1, testPlayerId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(totalChunks, rs.getInt("total_chunks"));
                }
            }
        }
    }

    @Test
    void testChunkDataOperations() throws SQLException {
        String testPlayerId = "550e8400-e29b-41d4-a716-446655440000";
        String world = "world";
        int chunkX = 10;
        int chunkZ = 20;
        
        try (Connection conn = DatabaseManager.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO global_chunks (world, chunk_x, chunk_z, discovered_by) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, world);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                stmt.setString(4, testPlayerId);
                assertEquals(1, stmt.executeUpdate());
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_chunks (player_id, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, testPlayerId);
                stmt.setString(2, world);
                stmt.setInt(3, chunkX);
                stmt.setInt(4, chunkZ);
                assertEquals(1, stmt.executeUpdate());
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM global_chunks WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                stmt.setString(1, world);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void testWorldBorderOperations() throws SQLException {
        String worldName = "test_world";
        double borderSize = 1000.0;
        
        try (Connection conn = DatabaseManager.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO world_borders (world_name, border_size) VALUES (?, ?)")) {
                stmt.setString(1, worldName);
                stmt.setDouble(2, borderSize);
                assertEquals(1, stmt.executeUpdate());
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT border_size FROM world_borders WHERE world_name = ?")) {
                stmt.setString(1, worldName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(borderSize, rs.getDouble("border_size"), 0.001);
                }
            }
        }
    }

    @Test
    void testDoubleInitializationThrowsException() throws DatabaseException {
        assertThrows(IllegalStateException.class, () -> {
            DatabaseManager.init(
                    TEST_HOST,
                    TEST_PORT,
                    TEST_DATABASE,
                    TEST_USERNAME,
                    TEST_PASSWORD
            );
        });
    }

    @Test
    void testGetDataSourceWithoutInitialization() {
        DatabaseManager.shutdown();
        assertThrows(IllegalStateException.class, DatabaseManager::getDataSource);
    }
}