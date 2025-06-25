package io.wax100.chunkDiscovery.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class RewardMessageHandlerTest {

    @Mock
    private Player player;
    
    @Mock
    private Server server;
    
    private RewardMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageHandler = new RewardMessageHandler();
        
        when(player.getName()).thenReturn("TestPlayer");
    }

    @Test
    void testSendWorldFirstMessage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getServer()).thenReturn(server);
            
            messageHandler.sendWorldFirstMessage(player, 42);
            
            verify(server).broadcastMessage(contains("TestPlayer"));
            verify(server).broadcastMessage(contains("42"));
            verify(server).broadcastMessage(contains("世界初発見"));
        }
    }

    @Test
    void testSendPersonalFirstMessage() {
        messageHandler.sendPersonalFirstMessage(player, 10);
        
        verify(player).sendMessage(contains("10"));
        verify(player).sendMessage(contains("チャンクを発見"));
    }

    @Test
    void testSendPersonalMilestoneMessage_Broadcast() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getServer()).thenReturn(server);
            
            messageHandler.sendPersonalMilestoneMessage(player, 25, "Custom message", true);
            
            verify(server).broadcastMessage(contains("TestPlayer"));
            verify(server).broadcastMessage(contains("25"));
            verify(server).broadcastMessage(contains("マイルストーン達成"));
        }
    }

    @Test
    void testSendPersonalMilestoneMessage_Personal() {
        messageHandler.sendPersonalMilestoneMessage(player, 25, "Custom message", false);
        
        verify(player).sendMessage(ChatColor.AQUA + "Custom message");
    }

    @Test
    void testSendPersonalMilestoneMessage_NoCustomMessage() {
        messageHandler.sendPersonalMilestoneMessage(player, 25, null, false);
        
        // カスタムメッセージがない場合は何も送信されない
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void testSendGlobalMilestoneMessage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getServer()).thenReturn(server);
            
            messageHandler.sendGlobalMilestoneMessage(100, "Global achievement!");
            
            verify(server).broadcastMessage(contains("100"));
            verify(server).broadcastMessage(contains("グローバルマイルストーン達成"));
            verify(server).broadcastMessage(ChatColor.YELLOW + "Global achievement!");
        }
    }

    @Test
    void testSendGlobalMilestoneMessage_NoCustomMessage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getServer()).thenReturn(server);
            
            messageHandler.sendGlobalMilestoneMessage(100, null);
            
            verify(server).broadcastMessage(contains("100"));
            verify(server).broadcastMessage(contains("グローバルマイルストーン達成"));
            // カスタムメッセージなしの場合は1回のみの呼び出し
            verify(server, times(1)).broadcastMessage(anyString());
        }
    }
}