package io.wax100.chunkDiscovery.listener;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChunkDiscoveryListenerTest {

    @Mock
    private DiscoveryService discoveryService;
    
    @Mock
    private Player player;
    
    @Mock
    private World world;
    
    @Mock
    private Chunk chunk;
    
    @Mock
    private Block block;
    
    @Mock
    private Location fromLocation;
    
    @Mock
    private Location toLocation;
    
    private ChunkDiscoveryListener listener;
    
    private final UUID playerId = UUID.randomUUID();
    private final String worldName = "test_world";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new ChunkDiscoveryListener(discoveryService);
        
        // Mock basic setup
        when(player.getUniqueId()).thenReturn(playerId);
        when(world.getName()).thenReturn(worldName);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
    }

    @Test
    void testPlayerJoinEvent() {
        // Setup
        PlayerJoinEvent event = new PlayerJoinEvent(player, "Welcome!");
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        
        // Execute
        listener.onPlayerJoin(event);
        
        // Verify that the player's initial position is recorded
        verify(player).getLocation();
        verify(toLocation).getChunk();
    }

    @Test
    void testPlayerQuitEvent() {
        // Setup - simulate player join first
        PlayerJoinEvent joinEvent = new PlayerJoinEvent(player, "Welcome!");
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        listener.onPlayerJoin(joinEvent);
        
        // Execute quit event
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "Goodbye!");
        listener.onPlayerQuit(quitEvent);
        
        // Verify cleanup happened (this is implicit in the implementation)
        assertDoesNotThrow(() -> listener.onPlayerQuit(quitEvent));
    }

    @Test
    void testPlayerMoveEvent_SameChunk() {
        // Setup - both locations in same chunk
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        
        // First move to establish position
        listener.onPlayerMove(event);
        
        // Second move to same chunk - should be ignored
        listener.onPlayerMove(event);
        
        // Verify discovery service is called only once
        verify(discoveryService, atMost(1)).isDiscovered(any(Player.class), any(Chunk.class));
    }

    @Test
    void testPlayerMoveEvent_NewChunk_AlreadyDiscovered() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(true);
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery service is checked but handleDiscovery is not called
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService, never()).handleDiscovery(player, chunk);
    }

    @Test
    void testPlayerMoveEvent_NewChunk_ValidForDiscovery() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        
        // Mock bedrock check - setup chunk with bedrock at bottom
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getBlockX()).thenReturn(0);
        when(toLocation.getBlockZ()).thenReturn(0);
        when(world.getBlockAt(0, -64, 0)).thenReturn(block);
        when(block.getType()).thenReturn(Material.BEDROCK);
        
        // Mock chunk bedrock check
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Block chunkBlock = mock(Block.class);
                when(chunk.getBlock(dx, -64, dz)).thenReturn(chunkBlock);
                when(chunkBlock.getType()).thenReturn(Material.BEDROCK);
            }
        }
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery process
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService).handleDiscovery(player, chunk);
    }

    @Test
    void testPlayerMoveEvent_NewChunk_InvalidForDiscovery_NotBedrock() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        
        // Mock bedrock check - setup chunk WITHOUT bedrock at bottom
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getBlockX()).thenReturn(0);
        when(toLocation.getBlockZ()).thenReturn(0);
        when(world.getBlockAt(0, -64, 0)).thenReturn(block);
        when(block.getType()).thenReturn(Material.STONE); // Not bedrock
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery is checked but not processed
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService, never()).handleDiscovery(player, chunk);
    }

    @Test
    void testPlayerMoveEvent_NetherEnvironment() {
        // Setup for Nether environment
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(world.getMinHeight()).thenReturn(0);
        
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        
        // Mock bedrock check for Nether
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getBlockX()).thenReturn(0);
        when(toLocation.getBlockZ()).thenReturn(0);
        when(world.getBlockAt(0, 0, 0)).thenReturn(block);
        when(block.getType()).thenReturn(Material.BEDROCK);
        
        // Mock chunk bedrock check for Nether
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Block chunkBlock = mock(Block.class);
                when(chunk.getBlock(dx, 0, dz)).thenReturn(chunkBlock);
                when(chunkBlock.getType()).thenReturn(Material.BEDROCK);
            }
        }
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery process works in Nether
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService).handleDiscovery(player, chunk);
    }

    @Test
    void testPlayerMoveEvent_EndEnvironment() {
        // Setup for End environment
        when(world.getEnvironment()).thenReturn(World.Environment.THE_END);
        
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        
        // Mock location for End
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(toLocation);
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery is checked but not processed (End is not valid)
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService, never()).handleDiscovery(player, chunk);
    }

    @Test
    void testPlayerMoveEvent_NullToLocation() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, null);
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify no discovery processing happens
        verify(discoveryService, never()).isDiscovered(any(Player.class), any(Chunk.class));
        verify(discoveryService, never()).handleDiscovery(any(Player.class), any(Chunk.class));
    }

    @Test
    void testPlayerMoveEvent_ExceptionHandling() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenThrow(new RuntimeException("Test exception"));
        
        // Execute - should not throw exception
        assertDoesNotThrow(() -> listener.onPlayerMove(event));
        
        // Verify no discovery processing happens due to exception
        verify(discoveryService, never()).isDiscovered(any(Player.class), any(Chunk.class));
        verify(discoveryService, never()).handleDiscovery(any(Player.class), any(Chunk.class));
    }

    @Test
    void testChunkBedrockValidation_PartialBedrock() {
        // Setup
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);
        when(toLocation.getChunk()).thenReturn(chunk);
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        
        // Mock bedrock check - player position has bedrock
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(toLocation);
        when(toLocation.getBlockX()).thenReturn(0);
        when(toLocation.getBlockZ()).thenReturn(0);
        when(world.getBlockAt(0, -64, 0)).thenReturn(block);
        when(block.getType()).thenReturn(Material.BEDROCK);
        
        // Mock chunk bedrock check - some blocks are not bedrock
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Block chunkBlock = mock(Block.class);
                when(chunk.getBlock(dx, -64, dz)).thenReturn(chunkBlock);
                // Make one block not bedrock
                if (dx == 5 && dz == 5) {
                    when(chunkBlock.getType()).thenReturn(Material.STONE);
                } else {
                    when(chunkBlock.getType()).thenReturn(Material.BEDROCK);
                }
            }
        }
        
        // Execute
        listener.onPlayerMove(event);
        
        // Verify discovery is not processed due to incomplete bedrock
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService, never()).handleDiscovery(player, chunk);
    }

    @Test
    void testConstructor_ValidService() {
        assertDoesNotThrow(() -> new ChunkDiscoveryListener(discoveryService));
    }

    @Test
    void testConstructor_NullService() {
        // ChunkDiscoveryListener doesn't validate null in constructor, it will fail later
        assertDoesNotThrow(() -> new ChunkDiscoveryListener(null));
    }
}