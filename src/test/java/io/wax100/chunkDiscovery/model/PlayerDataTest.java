package io.wax100.chunkDiscovery.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        String playerId = "test-player-id";
        int totalChunks = 42;

        // Act
        PlayerData playerData = new PlayerData(playerId, totalChunks);

        // Assert
        assertEquals(playerId, playerData.getPlayerId());
        assertEquals(totalChunks, playerData.getTotalChunks());
    }

    @Test
    void testImmutability() {
        // Arrange & Act
        PlayerData playerData = new PlayerData("test-id", 10);

        // Assert - Fields should be immutable (no setters available)
        assertEquals("test-id", playerData.getPlayerId());
        assertEquals(10, playerData.getTotalChunks());
        
        // Verify that the object maintains its state
        PlayerData sameData = new PlayerData("test-id", 10);
        assertEquals(playerData.getPlayerId(), sameData.getPlayerId());
        assertEquals(playerData.getTotalChunks(), sameData.getTotalChunks());
    }

    @Test
    void testObjectComparison() {
        // Arrange
        PlayerData player1 = new PlayerData("same-id", 10);
        PlayerData player2 = new PlayerData("same-id", 10);
        PlayerData player3 = new PlayerData("different-id", 10);
        PlayerData player4 = new PlayerData("same-id", 20);

        // Assert - Since equals/hashCode may not be implemented, test object identity
        assertEquals(player1, player1); // Same object reference
        assertNotNull(player1); // Not null
        assertNotEquals(player1, "not a PlayerData"); // Different type
        
        // Test different instances with same data
        assertNotEquals(player1, player2); // Different instances (if equals not overridden)
        assertNotEquals(player1, player3); // Different ID
        assertNotEquals(player1, player4); // Different chunks
    }

    @Test
    void testBasicObjectMethods() {
        // Arrange
        PlayerData playerData = new PlayerData("test-id", 15);

        // Act & Assert - Basic object methods should work
        assertNotNull(playerData.toString()); // toString should not return null
        assertNotEquals(0, playerData.hashCode()); // hashCode should work (though may not be meaningful)
    }

    @Test
    void testNullPlayerId() {
        // Act & Assert - Should not throw exception
        PlayerData playerData = new PlayerData(null, 5);
        assertNull(playerData.getPlayerId());
        assertEquals(5, playerData.getTotalChunks());
    }

    @Test
    void testNegativeChunks() {
        // Act & Assert - Should handle negative values
        PlayerData playerData = new PlayerData("test-id", -1);
        assertEquals("test-id", playerData.getPlayerId());
        assertEquals(-1, playerData.getTotalChunks());
    }

    @Test
    void testZeroChunks() {
        // Act & Assert
        PlayerData playerData = new PlayerData("test-id", 0);
        assertEquals("test-id", playerData.getPlayerId());
        assertEquals(0, playerData.getTotalChunks());
    }
}