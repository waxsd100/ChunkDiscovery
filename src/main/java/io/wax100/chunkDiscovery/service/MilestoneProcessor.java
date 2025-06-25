package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.manager.EffectManager;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.manager.RewardManager;
import io.wax100.chunkDiscovery.model.RewardItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * マイルストーン処理を担当するクラス
 */
public class MilestoneProcessor {
    private final ChunkDiscoveryPlugin plugin;
    private final RewardManager rewardManager;
    private final RewardMessageHandler messageHandler;
    private final Set<Integer> triggeredGlobalMilestones;
    
    public MilestoneProcessor(ChunkDiscoveryPlugin plugin, RewardManager rewardManager, 
                             RewardMessageHandler messageHandler, Set<Integer> triggeredGlobalMilestones) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
        this.messageHandler = messageHandler;
        this.triggeredGlobalMilestones = triggeredGlobalMilestones;
    }
    
    /**
     * 個人マイルストーンをチェックして処理
     */
    public void processPersonalMilestones(Player player, int totalChunks) {
        logDebug("個人マイルストーンチェック: " + player.getName() + ", チャンク数=" + totalChunks);
        
        MilestoneConfig.MilestoneEntry milestone = findMatchingMilestone(MilestoneConfig.personal, totalChunks);
        if (milestone == null) {
            return;
        }
        
        executePersonalMilestone(player, milestone, totalChunks);
    }
    
    /**
     * グローバルマイルストーンをチェックして処理
     */
    public void processGlobalMilestones(int totalDiscoveredChunks) {
        logDebug("グローバルマイルストーンチェック: チャンク数=" + totalDiscoveredChunks);
        
        MilestoneConfig.MilestoneEntry milestone = findMatchingMilestone(MilestoneConfig.global, totalDiscoveredChunks);
        if (milestone == null || triggeredGlobalMilestones.contains(milestone.discoveryCount)) {
            return;
        }
        
        // 重複実行を防ぐ
        triggeredGlobalMilestones.add(milestone.discoveryCount);
        
        // メインスレッドで実行
        Bukkit.getScheduler().runTask(plugin, () -> executeGlobalMilestone(milestone, totalDiscoveredChunks));
    }
    
    /**
     * グローバルマイルストーンの履歴をリセット
     */
    public void resetGlobalMilestoneHistory() {
        triggeredGlobalMilestones.clear();
        plugin.getLogger().info("グローバルマイルストーンの履歴がリセットされました。");
    }
    
    // Private helper methods
    
    private MilestoneConfig.MilestoneEntry findMatchingMilestone(Iterable<MilestoneConfig.MilestoneEntry> milestones, int targetCount) {
        for (MilestoneConfig.MilestoneEntry milestone : milestones) {
            if (milestone.discoveryCount == targetCount) {
                return milestone;
            }
        }
        return null;
    }
    
    private void executePersonalMilestone(Player player, MilestoneConfig.MilestoneEntry milestone, int totalChunks) {
        try {
            // 報酬アイテム付与
            giveRewardItems(player, milestone.items);
            
            // メッセージ送信
            if (milestone.sendMessage) {
                messageHandler.sendPersonalMilestoneMessage(player, totalChunks, milestone.message, milestone.broadcast);
            }
            
            // エフェクト再生
            if (milestone.playEffects) {
                EffectManager.spawnFirework(player.getLocation());
            }
            
            plugin.getLogger().info(player.getName() + " が個人マイルストーン " + totalChunks + " チャンクを達成");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "個人マイルストーン処理中にエラー", e);
        }
    }
    
    private void executeGlobalMilestone(MilestoneConfig.MilestoneEntry milestone, int totalDiscoveredChunks) {
        try {
            // 全プレイヤーに報酬配布
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                giveRewardItems(onlinePlayer, milestone.items);
                
                // エフェクト再生
                if (milestone.playEffects) {
                    EffectManager.spawnFirework(onlinePlayer.getLocation());
                }
            }
            
            // グローバルメッセージ送信
            if (milestone.sendMessage) {
                messageHandler.sendGlobalMilestoneMessage(totalDiscoveredChunks, milestone.message);
            }
            
            plugin.getLogger().info("グローバルマイルストーン " + totalDiscoveredChunks + " チャンクが達成");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "グローバルマイルストーン処理中にエラー", e);
        }
    }
    
    private void giveRewardItems(Player player, List<RewardItem> items) {
        if (items != null && !items.isEmpty()) {
            for (RewardItem rewardItem : items) {
                rewardManager.giveRewardItem(player, rewardItem);
            }
        }
    }
    
    private void logDebug(String message) {
        plugin.getLogger().log(Level.FINE, message);
    }
}