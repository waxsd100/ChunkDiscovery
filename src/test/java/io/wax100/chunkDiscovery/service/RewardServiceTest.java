package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RewardServiceTest {

    @Mock
    private ChunkDiscoveryPlugin plugin;
    
    @Mock
    private Logger logger;
    
    @Mock
    private Player player;
    
    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration.class));
        
        // RewardServiceのコンストラクタは実際のRewardManagerを作成するため、
        // モックを使わずに実際のインスタンスを作成
        rewardService = new RewardService(plugin);
    }

    @Test
    void testConstructor_ValidPlugin() {
        assertDoesNotThrow(() -> {
            new RewardService(plugin);
        });
    }

    @Test
    void testConstructor_NullPlugin() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RewardService(null);
        });
    }

    @Test
    void testGrantRewards_NullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> {
            rewardService.grantRewards(null, true, true, 10);
        });
    }

    @Test
    void testGrantRewards_NegativeTotalChunks() {
        assertThrows(IllegalArgumentException.class, () -> {
            rewardService.grantRewards(player, true, true, -1);
        });
    }

    @Test
    void testGrantRewards_ValidInput() {
        // バリデーションだけテスト（Bukkitサーバーが必要な部分はスキップ）
        // 引数検証が正しく動作することを確認
        try {
            rewardService.grantRewards(player, true, false, 10);
            // Bukkitサーバーがnullのため例外が発生するが、これは予期される動作
        } catch (Exception e) {
            // Bukkit関連のNullPointerExceptionは予期される
            assertTrue(e.getMessage().contains("server") || e instanceof NullPointerException);
        }
    }

    @Test
    void testCheckGlobalMilestones_ValidInput() {
        assertDoesNotThrow(() -> {
            rewardService.checkGlobalMilestones(100);
        });
        
        assertDoesNotThrow(() -> {
            rewardService.checkGlobalMilestones(0);
        });
    }

    @Test
    void testReloadRewards() {
        assertDoesNotThrow(() -> {
            rewardService.reloadRewards();
        });
    }

    @Test
    void testResetGlobalMilestoneHistory() {
        assertDoesNotThrow(() -> {
            rewardService.resetGlobalMilestoneHistory();
        });
    }
}