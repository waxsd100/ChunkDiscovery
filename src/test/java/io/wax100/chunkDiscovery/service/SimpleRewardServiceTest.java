package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleRewardServiceTest {

    @Mock
    private ChunkDiscoveryPlugin mockPlugin;
    
    @Mock
    private Logger mockLogger;

    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        rewardService = new RewardService(mockPlugin);
    }

    @Test
    void testRewardServiceCreation() {
        // Assert
        assertNotNull(rewardService);
        verify(mockPlugin, atLeastOnce()).getLogger();
    }

    @Test
    void testReloadRewards() {
        // Act
        rewardService.reloadRewards();

        // Assert
        verify(mockLogger).info("報酬設定がリロードされました。");
    }

    @Test
    void testResetGlobalMilestoneHistory() {
        // Act
        rewardService.resetGlobalMilestoneHistory();

        // Assert
        verify(mockLogger).info("グローバルマイルストーンの履歴がリセットされました。");
    }
}