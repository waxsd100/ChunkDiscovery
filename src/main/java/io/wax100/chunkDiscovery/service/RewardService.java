package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.manager.EffectManager;
import io.wax100.chunkDiscovery.manager.RewardManager;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.model.RewardItem;
import io.wax100.chunkDiscovery.util.ErrorHandler;
import io.wax100.chunkDiscovery.util.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 報酬配布を統括するサービスクラス
 * 
 * 責任：
 * - チャンク発見時の報酬配布
 * - 個人・グローバルマイルストーンの管理
 * - 報酬設定のリロード
 */
public class RewardService {
    private final ChunkDiscoveryPlugin plugin;
    private final RewardManager rewardManager;
    private final RewardMessageHandler messageHandler;
    private final Set<Integer> triggeredGlobalMilestones = ConcurrentHashMap.newKeySet();
    private MilestoneProcessor milestoneProcessor;

    public RewardService(ChunkDiscoveryPlugin plugin) {
        this.plugin = Validate.requireNonNull(plugin, "Plugin cannot be null");
        this.rewardManager = new RewardManager(plugin);
        this.messageHandler = new RewardMessageHandler();
        
        initializeRewardSystem();
    }
    
    private void initializeRewardSystem() {
        try {
            this.rewardManager.loadMilestoneRewards();
            plugin.getLogger().info("報酬システムが正常に初期化されました");
        } catch (Exception e) {
            ErrorHandler.logError(e, plugin.getLogger(), "報酬設定の読み込み");
        }
    }

    /**
     * 発見時の報酬を配布する
     * @param player 発見者
     * @param globalFirst 世界初発見かどうか
     * @param personalFirst 個人初発見かどうか
     * @param totalChunks 現在の発見総数
     */
    public void grantRewards(Player player, boolean globalFirst, boolean personalFirst, int totalChunks) {
        Validate.requireNonNull(player, "Player cannot be null");
        Validate.requireNonNegative(totalChunks, "Total chunks must be non-negative");
        
        DiscoveryType discoveryType = determineDiscoveryType(globalFirst, personalFirst);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                processDiscoveryReward(player, discoveryType, totalChunks);
                getMilestoneProcessor().processPersonalMilestones(player, totalChunks);
            } catch (Exception e) {
                ErrorHandler.handleAndNotify(e, plugin.getLogger(), player, "報酬付与");
            }
        });
    }
    
    private DiscoveryType determineDiscoveryType(boolean globalFirst, boolean personalFirst) {
        if (globalFirst) {
            return DiscoveryType.WORLD_FIRST;
        } else if (personalFirst) {
            return DiscoveryType.PERSONAL_FIRST;
        } else {
            return DiscoveryType.ALREADY_DISCOVERED;
        }
    }
    
    private void processDiscoveryReward(Player player, DiscoveryType type, int totalChunks) {
        switch (type) {
            case WORLD_FIRST -> grantWorldFirstRewards(player, totalChunks);
            case PERSONAL_FIRST -> grantPersonalFirstRewards(player, totalChunks);
            case ALREADY_DISCOVERED -> {
                // 既発見の場合は基本報酬なし（マイルストーンのみチェック）
            }
        }
    }
    
    /**
     * 世界初発見時の報酬付与
     */
    private void grantWorldFirstRewards(Player player, int totalChunks) {
        rewardManager.giveWorldFirstRewards(player);
        EffectManager.celebrateMilestone(player.getLocation());
        messageHandler.sendWorldFirstMessage(player, totalChunks);
    }
    
    /**
     * 個人初発見時の報酬付与
     */
    private void grantPersonalFirstRewards(Player player, int totalChunks) {
        rewardManager.givePersonalRewards(player);
        EffectManager.spawnFirework(player.getLocation());
        messageHandler.sendPersonalFirstMessage(player, totalChunks);
    }


    /**
     * グローバルマイルストーンをチェックして報酬を付与
     */
    public void checkGlobalMilestones(int totalDiscoveredChunks) {
        getMilestoneProcessor().processGlobalMilestones(totalDiscoveredChunks);
    }

    /**
     * 報酬設定をリロード
     */
    public void reloadRewards() {
        try {
            rewardManager.loadMilestoneRewards();
            getMilestoneProcessor().resetGlobalMilestoneHistory();
            plugin.getLogger().info("報酬設定がリロードされました。");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "報酬設定のリロード中にエラー", e);
        }
    }

    /**
     * グローバルマイルストーンのトリガー履歴をリセット（テスト用）
     */
    public void resetGlobalMilestoneHistory() {
        getMilestoneProcessor().resetGlobalMilestoneHistory();
    }
    
    /**
     * マイルストーンプロセッサーを取得（遅延初期化）
     */
    private MilestoneProcessor getMilestoneProcessor() {
        if (milestoneProcessor == null) {
            milestoneProcessor = new MilestoneProcessor(plugin, rewardManager, messageHandler, triggeredGlobalMilestones);
        }
        return milestoneProcessor;
    }
}