package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.model.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {

    @Mock
    private PlayerRepository playerRepo;
    
    @Mock
    private ChunkRepository chunkRepo;
    
    @Mock
    private RewardService rewardService;
    
    @Mock
    private ChunkDiscoveryPlugin plugin;
    
    @Mock
    private Logger logger;
    
    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        discoveryService = new DiscoveryService(playerRepo, chunkRepo, rewardService, plugin);
    }

    @Test
    void testConstructor_ValidArguments() {
        assertDoesNotThrow(() -> {
            new DiscoveryService(playerRepo, chunkRepo, rewardService, plugin);
        });
    }

    @Test
    void testConstructor_NullPlayerRepo() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(null, chunkRepo, rewardService, plugin);
        });
    }

    @Test
    void testConstructor_NullChunkRepo() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, null, rewardService, plugin);
        });
    }

    @Test
    void testConstructor_NullRewardService() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, chunkRepo, null, plugin);
        });
    }

    @Test
    void testConstructor_NullPlugin() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, chunkRepo, rewardService, null);
        });
    }

    @Test
    void testGetPlayerTotalChunksAsync_ValidPlayerId() throws ExecutionException, InterruptedException {
        String playerId = "test-player-id";
        int expectedChunks = 42;
        
        when(playerRepo.getTotalChunks(playerId)).thenReturn(expectedChunks);
        
        CompletableFuture<Integer> future = discoveryService.getPlayerTotalChunksAsync(playerId);
        Integer result = future.get();
        
        assertEquals(expectedChunks, result);
        verify(playerRepo).getTotalChunks(playerId);
    }

    @Test
    void testGetPlayerTotalChunksAsync_NullPlayerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync(null);
        });
    }

    @Test
    void testGetPlayerTotalChunksAsync_EmptyPlayerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync("");
        });
    }

    @Test
    void testGetPlayerTotalChunksAsync_WhitespacePlayerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync("   ");
        });
    }

    @Test
    void testGetPlayerTotalChunksAsync_RepositoryException() throws ExecutionException, InterruptedException {
        String playerId = "test-player-id";
        
        when(playerRepo.getTotalChunks(playerId)).thenThrow(new RuntimeException("Database error"));
        
        CompletableFuture<Integer> future = discoveryService.getPlayerTotalChunksAsync(playerId);
        Integer result = future.get();
        
        assertEquals(0, result); // Default value on error
    }

    @Test
    void testGetPlayerWorldChunksAsync_ValidArguments() throws ExecutionException, InterruptedException {
        String playerId = "test-player-id";
        String worldName = "test-world";
        int expectedChunks = 15;
        
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName)).thenReturn(expectedChunks);
        
        CompletableFuture<Integer> future = discoveryService.getPlayerWorldChunksAsync(playerId, worldName);
        Integer result = future.get();
        
        assertEquals(expectedChunks, result);
        verify(playerRepo).getPlayerChunksInWorld(playerId, worldName);
    }

    @Test
    void testGetPlayerWorldChunksAsync_NullPlayerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync(null, "test-world");
        });
    }

    @Test
    void testGetPlayerWorldChunksAsync_NullWorldName() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync("test-player-id", null);
        });
    }

    @Test
    void testGetPlayerWorldChunksAsync_EmptyPlayerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync("", "test-world");
        });
    }

    @Test
    void testGetPlayerWorldChunksAsync_EmptyWorldName() {
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync("test-player-id", "");
        });
    }

    @Test
    void testGetPlayerWorldChunksAsync_RepositoryException() throws ExecutionException, InterruptedException {
        String playerId = "test-player-id";
        String worldName = "test-world";
        
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName))
            .thenThrow(new RuntimeException("Database error"));
        
        CompletableFuture<Integer> future = discoveryService.getPlayerWorldChunksAsync(playerId, worldName);
        Integer result = future.get();
        
        assertEquals(0, result); // Default value on error
    }

    @Test
    void testGetPlayerTotalChunks_Synchronous() {
        String playerId = "test-player-id";
        int expectedChunks = 42;
        
        when(playerRepo.getTotalChunks(playerId)).thenReturn(expectedChunks);
        
        int result = discoveryService.getPlayerTotalChunks(playerId);
        
        assertEquals(expectedChunks, result);
        verify(playerRepo).getTotalChunks(playerId);
    }

    @Test
    void testGetPlayerTotalChunks_RepositoryException() {
        String playerId = "test-player-id";
        
        when(playerRepo.getTotalChunks(playerId)).thenThrow(new RuntimeException("Database error"));
        
        int result = discoveryService.getPlayerTotalChunks(playerId);
        
        assertEquals(0, result); // Default value on error
    }

    @Test
    void testGetTopPlayers_Success() {
        int limit = 10;
        List<PlayerData> expectedPlayers = List.of(
            new PlayerData("player1", 100),
            new PlayerData("player2", 90)
        );
        
        when(playerRepo.getTopPlayers(limit)).thenReturn(expectedPlayers);
        
        List<PlayerData> result = discoveryService.getTopPlayers(limit);
        
        assertEquals(expectedPlayers, result);
        verify(playerRepo).getTopPlayers(limit);
    }

    @Test
    void testGetTopPlayers_RepositoryException() {
        int limit = 10;
        
        when(playerRepo.getTopPlayers(limit)).thenThrow(new RuntimeException("Database error"));
        
        List<PlayerData> result = discoveryService.getTopPlayers(limit);
        
        assertTrue(result.isEmpty()); // Default empty list on error
    }

    @Test
    void testGetPlayerChunksInWorld_Success() {
        String playerId = "test-player-id";
        String worldName = "test-world";
        int expectedChunks = 25;
        
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName)).thenReturn(expectedChunks);
        
        int result = discoveryService.getPlayerChunksInWorld(playerId, worldName);
        
        assertEquals(expectedChunks, result);
        verify(playerRepo).getPlayerChunksInWorld(playerId, worldName);
    }

    @Test
    void testGetPlayerChunksInWorld_RepositoryException() {
        String playerId = "test-player-id";
        String worldName = "test-world";
        
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName))
            .thenThrow(new RuntimeException("Database error"));
        
        int result = discoveryService.getPlayerChunksInWorld(playerId, worldName);
        
        assertEquals(0, result); // Default value on error
    }
}