package io.wax100.chunkDiscovery.integration;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.model.PlayerData;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.service.RewardService;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DiscoveryServiceのリファクタリング後の統合テスト
 */
class DiscoveryIntegrationTest {

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
    
    @Mock
    private Player player;
    
    @Mock
    private Chunk chunk;
    
    @Mock
    private World world;
    
    private DiscoveryService discoveryService;
    
    private final String playerId = UUID.randomUUID().toString();
    private final String worldName = "test_world";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(UUID.fromString(playerId));
        when(chunk.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn(worldName);
        
        discoveryService = new DiscoveryService(playerRepo, chunkRepo, rewardService, plugin);
    }

    @Test
    void testValidationInConstructor() {
        // すべてのパラメータがnullでないことが検証される
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(null, chunkRepo, rewardService, plugin);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, null, rewardService, plugin);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, chunkRepo, null, plugin);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DiscoveryService(playerRepo, chunkRepo, rewardService, null);
        });
    }

    @Test
    void testAsyncMethodsWithValidInput() throws ExecutionException, InterruptedException {
        // プレイヤー統計の非同期取得テスト
        when(playerRepo.getTotalChunks(playerId)).thenReturn(42);
        
        CompletableFuture<Integer> totalChunksFuture = discoveryService.getPlayerTotalChunksAsync(playerId);
        Integer totalChunks = totalChunksFuture.get();
        
        assertEquals(42, totalChunks);
        verify(playerRepo).getTotalChunks(playerId);
        
        // ワールド別統計の非同期取得テスト
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName)).thenReturn(15);
        
        CompletableFuture<Integer> worldChunksFuture = discoveryService.getPlayerWorldChunksAsync(playerId, worldName);
        Integer worldChunks = worldChunksFuture.get();
        
        assertEquals(15, worldChunks);
        verify(playerRepo).getPlayerChunksInWorld(playerId, worldName);
    }

    @Test
    void testAsyncMethodsWithInvalidInput() {
        // 無効な入力に対する検証テスト
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerTotalChunksAsync("   ");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync(null, worldName);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync(playerId, null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync("", worldName);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            discoveryService.getPlayerWorldChunksAsync(playerId, "");
        });
    }

    @Test
    void testErrorHandlingInAsyncMethods() throws ExecutionException, InterruptedException {
        // リポジトリで例外が発生した場合のデフォルト値返却テスト
        when(playerRepo.getTotalChunks(playerId)).thenThrow(new RuntimeException("Database error"));
        
        CompletableFuture<Integer> future = discoveryService.getPlayerTotalChunksAsync(playerId);
        Integer result = future.get();
        
        assertEquals(0, result); // デフォルト値
        
        // ワールド別統計でも同様
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName))
            .thenThrow(new RuntimeException("Database error"));
        
        CompletableFuture<Integer> worldFuture = discoveryService.getPlayerWorldChunksAsync(playerId, worldName);
        Integer worldResult = worldFuture.get();
        
        assertEquals(0, worldResult); // デフォルト値
    }

    @Test
    void testSynchronousMethodsConsistency() {
        // 同期版メソッドが非同期版と同じ結果を返すことを確認
        when(playerRepo.getTotalChunks(playerId)).thenReturn(42);
        when(playerRepo.getPlayerChunksInWorld(playerId, worldName)).thenReturn(15);
        
        int syncTotal = discoveryService.getPlayerTotalChunks(playerId);
        int syncWorld = discoveryService.getPlayerChunksInWorld(playerId, worldName);
        
        assertEquals(42, syncTotal);
        assertEquals(15, syncWorld);
        
        verify(playerRepo, times(1)).getTotalChunks(playerId);
        verify(playerRepo, times(1)).getPlayerChunksInWorld(playerId, worldName);
    }

    @Test
    void testIsDiscoveredMethod() {
        // 発見済みチェックのテスト
        when(playerRepo.hasDiscoveredChunk(playerId, chunk)).thenReturn(true);
        
        boolean isDiscovered = discoveryService.isDiscovered(player, chunk);
        
        assertTrue(isDiscovered);
        verify(playerRepo).hasDiscoveredChunk(playerId, chunk);
    }

    @Test
    void testIsDiscoveredMethodWithException() {
        // 例外発生時は未発見として扱うことを確認
        when(playerRepo.hasDiscoveredChunk(playerId, chunk))
            .thenThrow(new RuntimeException("Database error"));
        
        boolean isDiscovered = discoveryService.isDiscovered(player, chunk);
        
        assertFalse(isDiscovered); // エラー時は未発見として扱う
    }

}