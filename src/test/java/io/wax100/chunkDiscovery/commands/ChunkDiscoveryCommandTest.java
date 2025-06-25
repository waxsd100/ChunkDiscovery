package io.wax100.chunkDiscovery.commands;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.model.PlayerData;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChunkDiscoveryCommandTest {

    @Mock
    private DiscoveryService discoveryService;
    
    @Mock
    private ChunkDiscoveryPlugin plugin;
    
    @Mock
    private CommandSender consoleSender;
    
    @Mock
    private Player player;
    
    @Mock
    private Player targetPlayer;
    
    @Mock
    private Command command;
    
    @Mock
    private World world;
    
    @Mock
    private Location location;
    
    @Mock
    private Chunk chunk;
    
    @Mock
    private Logger logger;
    
    @Mock
    private PluginDescriptionFile description;
    
    @Mock
    private BukkitScheduler scheduler;
    
    private ChunkDiscoveryCommand chunkCommand;
    
    private final UUID playerId = UUID.randomUUID();
    private final UUID targetPlayerId = UUID.randomUUID();
    private final String worldName = "test_world";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chunkCommand = new ChunkDiscoveryCommand(discoveryService, plugin);
        
        // Mock basic setup
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getVersion()).thenReturn("1.0-TEST");
        when(description.getAuthors()).thenReturn(Arrays.asList("TestAuthor"));
        
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn(worldName);
        
        when(targetPlayer.getUniqueId()).thenReturn(targetPlayerId);
        when(targetPlayer.getName()).thenReturn("TargetPlayer");
    }

    @Test
    void testOnCommand_NoArguments() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("Usage: /chunkdiscovery"));
    }

    @Test
    void testOnCommand_UnknownSubcommand() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"unknown"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("Unknown subcommand: unknown"));
        verify(consoleSender).sendMessage(contains("Available commands:"));
    }

    @Test
    void testStatsCommand_PlayerOnly() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"stats"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("このコマンドはプレイヤーのみ実行可能です"));
    }

    @Test
    void testStatsCommand_SelfStats() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            // Mock async methods
            when(discoveryService.getPlayerTotalChunksAsync(playerId.toString()))
                .thenReturn(CompletableFuture.completedFuture(42));
            when(discoveryService.getPlayerWorldChunksAsync(playerId.toString(), worldName))
                .thenReturn(CompletableFuture.completedFuture(15));
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", new String[]{"stats"});
            
            assertTrue(result);
            verify(discoveryService).getPlayerTotalChunksAsync(playerId.toString());
            verify(discoveryService).getPlayerWorldChunksAsync(playerId.toString(), worldName);
        }
    }

    @Test
    void testStatsCommand_TargetPlayerStats() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(targetPlayer);
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            when(discoveryService.getPlayerTotalChunksAsync(targetPlayerId.toString()))
                .thenReturn(CompletableFuture.completedFuture(100));
            when(discoveryService.getPlayerWorldChunksAsync(targetPlayerId.toString(), worldName))
                .thenReturn(CompletableFuture.completedFuture(25));
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", 
                new String[]{"stats", "TargetPlayer"});
            
            assertTrue(result);
            verify(discoveryService).getPlayerTotalChunksAsync(targetPlayerId.toString());
            verify(discoveryService).getPlayerWorldChunksAsync(targetPlayerId.toString(), worldName);
        }
    }

    @Test
    void testStatsCommand_PlayerNotFound() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("UnknownPlayer")).thenReturn(null);
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", 
                new String[]{"stats", "UnknownPlayer"});
            
            assertTrue(result);
            verify(player).sendMessage(contains("プレイヤー UnknownPlayer が見つかりません"));
        }
    }

    @Test
    void testTopCommand_DefaultLimit() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            List<PlayerData> topPlayers = Arrays.asList(
                new PlayerData("player1", 100),
                new PlayerData("player2", 90),
                new PlayerData("player3", 80)
            );
            
            when(discoveryService.getTopPlayersAsync(10))
                .thenReturn(CompletableFuture.completedFuture(topPlayers));
            
            boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"top"});
            
            assertTrue(result);
            verify(discoveryService).getTopPlayersAsync(10);
        }
    }

    @Test
    void testTopCommand_CustomLimit() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            List<PlayerData> topPlayers = Arrays.asList(
                new PlayerData("player1", 100),
                new PlayerData("player2", 90)
            );
            
            when(discoveryService.getTopPlayersAsync(5))
                .thenReturn(CompletableFuture.completedFuture(topPlayers));
            
            boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", 
                new String[]{"top", "5"});
            
            assertTrue(result);
            verify(discoveryService).getTopPlayersAsync(5);
        }
    }

    @Test
    void testTopCommand_InvalidLimit() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", 
            new String[]{"top", "invalid"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("無効な数値です: invalid"));
    }

    @Test
    void testTopCommand_EmptyResult() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            when(discoveryService.getTopPlayersAsync(10))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
            
            boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"top"});
            
            assertTrue(result);
            verify(discoveryService).getTopPlayersAsync(10);
        }
    }

    @Test
    void testInfoCommand() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"info"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("ChunkDiscovery Plugin 情報"));
        verify(consoleSender).sendMessage(contains("バージョン: 1.0-TEST"));
        verify(consoleSender).sendMessage(contains("作者: TestAuthor"));
    }

    @Test
    void testCheckCommand_PlayerOnly() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"check"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("このコマンドはプレイヤーのみ実行可能です"));
    }

    @Test
    void testCheckCommand_ChunkDiscovered() {
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(true);
        when(discoveryService.getPlayerChunksInWorld(playerId.toString(), worldName)).thenReturn(25);
        
        boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", new String[]{"check"});
        
        assertTrue(result);
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService).getPlayerChunksInWorld(playerId.toString(), worldName);
        verify(player).sendMessage(contains("既に発見済み"));
        verify(player).sendMessage(contains("での発見数: "));
    }

    @Test
    void testCheckCommand_ChunkNotDiscovered() {
        when(discoveryService.isDiscovered(player, chunk)).thenReturn(false);
        when(discoveryService.getPlayerChunksInWorld(playerId.toString(), worldName)).thenReturn(10);
        
        boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", new String[]{"check"});
        
        assertTrue(result);
        verify(discoveryService).isDiscovered(player, chunk);
        verify(discoveryService).getPlayerChunksInWorld(playerId.toString(), worldName);
        verify(player).sendMessage(contains("未発見"));
    }

    @Test
    void testReloadCommand_WithoutPermission() {
        when(consoleSender.hasPermission("chunkdiscovery.reload")).thenReturn(false);
        
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"reload"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("このコマンドを実行する権限がありません"));
    }

    @Test
    void testReloadCommand_WithPermission() {
        when(consoleSender.hasPermission("chunkdiscovery.reload")).thenReturn(true);
        
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"reload"});
        
        assertTrue(result);
        verify(plugin).reloadPluginConfig();
        verify(consoleSender).sendMessage(contains("設定のリロードが完了しました"));
    }

    @Test
    void testReloadCommand_Exception() {
        when(consoleSender.hasPermission("chunkdiscovery.reload")).thenReturn(true);
        doThrow(new RuntimeException("Test exception")).when(plugin).reloadPluginConfig();
        
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"reload"});
        
        assertTrue(result);
        verify(plugin).reloadPluginConfig();
        verify(consoleSender).sendMessage(contains("設定のリロード中にエラーが発生しました"));
        verify(logger).severe(contains("設定リロード中にエラーが発生しました"));
    }

    @Test
    void testWorldCommand_PlayerOnly() {
        boolean result = chunkCommand.onCommand(consoleSender, command, "chunkdiscovery", new String[]{"world"});
        
        assertTrue(result);
        verify(consoleSender).sendMessage(contains("このコマンドはプレイヤーのみ実行可能です"));
    }

    @Test
    void testWorldCommand_NoWorldSpecified() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorlds()).thenReturn(Arrays.asList(world));
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", new String[]{"world"});
            
            assertTrue(result);
            verify(player).sendMessage(contains("Usage: /chunkdiscovery world"));
            verify(player).sendMessage(contains("利用可能なワールド:"));
        }
    }

    @Test
    void testWorldCommand_InvalidWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("invalid_world")).thenReturn(null);
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", 
                new String[]{"world", "invalid_world"});
            
            assertTrue(result);
            verify(player).sendMessage(contains("ワールド 'invalid_world' が見つかりません"));
        }
    }

    @Test
    void testWorldCommand_ValidWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
            
            when(discoveryService.getPlayerWorldChunksAsync(playerId.toString(), "test_world"))
                .thenReturn(CompletableFuture.completedFuture(30));
            
            boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", 
                new String[]{"world", "test_world"});
            
            assertTrue(result);
            verify(discoveryService).getPlayerWorldChunksAsync(playerId.toString(), "test_world");
        }
    }

    @Test
    void testOnTabComplete_FirstArgument() {
        List<String> completions = chunkCommand.onTabComplete(consoleSender, command, "chunkdiscovery", 
            new String[]{"st"});
        
        assertTrue(completions.contains("stats"));
        assertFalse(completions.contains("reload")); // No permission
    }

    @Test
    void testOnTabComplete_FirstArgument_WithReloadPermission() {
        when(consoleSender.hasPermission("chunkdiscovery.reload")).thenReturn(true);
        
        List<String> completions = chunkCommand.onTabComplete(consoleSender, command, "chunkdiscovery", 
            new String[]{"re"});
        
        assertTrue(completions.contains("reload"));
    }

    @Test
    void testOnTabComplete_StatsPlayerName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOnlinePlayers()).thenReturn(Arrays.asList(player, targetPlayer));
            
            List<String> completions = chunkCommand.onTabComplete(consoleSender, command, "chunkdiscovery", 
                new String[]{"stats", "Test"});
            
            assertTrue(completions.contains("TestPlayer"));
            assertFalse(completions.contains("TargetPlayer"));
        }
    }

    @Test
    void testOnTabComplete_WorldName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorlds()).thenReturn(Arrays.asList(world));
            
            List<String> completions = chunkCommand.onTabComplete(consoleSender, command, "chunkdiscovery", 
                new String[]{"world", "test"});
            
            assertTrue(completions.contains("test_world"));
        }
    }

    @Test
    void testOnTabComplete_NoMatch() {
        List<String> completions = chunkCommand.onTabComplete(consoleSender, command, "chunkdiscovery", 
            new String[]{"invalid", "arg"});
        
        assertTrue(completions.isEmpty());
    }

    @Test
    void testCommandExceptionHandling() {
        when(discoveryService.getPlayerTotalChunksAsync(anyString()))
            .thenThrow(new RuntimeException("Test exception"));
        
        boolean result = chunkCommand.onCommand(player, command, "chunkdiscovery", new String[]{"stats"});
        
        assertTrue(result);
        verify(logger).severe(contains("コマンド実行中にエラーが発生しました"));
        verify(player).sendMessage(contains("コマンドの実行中にエラーが発生しました"));
    }

    @Test
    void testConstructor_ValidArguments() {
        assertDoesNotThrow(() -> new ChunkDiscoveryCommand(discoveryService, plugin));
    }

    @Test
    void testConstructor_NullService() {
        // ChunkDiscoveryCommand doesn't validate null in constructor
        assertDoesNotThrow(() -> new ChunkDiscoveryCommand(null, plugin));
    }

    @Test
    void testConstructor_NullPlugin() {
        // ChunkDiscoveryCommand doesn't validate null in constructor
        assertDoesNotThrow(() -> new ChunkDiscoveryCommand(discoveryService, null));
    }
}