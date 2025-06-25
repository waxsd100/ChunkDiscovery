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

/**
 * 報酬配布を統括するサービスクラス（レガシーAPI対応）
 */
public class RewardService {
    private final ChunkDiscoveryPlugin plugin;
    private final RewardManager rewardManager;
    private final Set<Integer> triggeredGlobalMilestones = ConcurrentHashMap.newKeySet();

    public RewardService(ChunkDiscoveryPlugin plugin) {
        this.plugin = Validate.requireNonNull(plugin, "Plugin cannot be null");
        this.rewardManager = new RewardManager(plugin);
        try {
            this.rewardManager.loadMilestoneRewards();
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
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (globalFirst) {
                    grantWorldFirstRewards(player, totalChunks);
                } else if (personalFirst) {
                    grantPersonalFirstRewards(player, totalChunks);
                }

                // 個人マイルストーンチェック
                checkPersonalMilestones(player, totalChunks);

            } catch (Exception e) {
                ErrorHandler.handleAndNotify(e, plugin.getLogger(), player, "報酬付与");
            }
        });
    }
    
    /**
     * 世界初発見時の報酬付与
     */
    private void grantWorldFirstRewards(Player player, int totalChunks) {
        rewardManager.giveWorldFirstRewards(player);
        EffectManager.celebrateMilestone(player.getLocation());
        
        String message = ChatColor.WHITE + "" + ChatColor.BOLD + player.getName() + 
                        " さんが " + ChatColor.LIGHT_PURPLE + totalChunks + " 番目のチャンクを世界初発見！";
        Bukkit.getServer().broadcastMessage(message);
    }
    
    /**
     * 個人初発見時の報酬付与
     */
    private void grantPersonalFirstRewards(Player player, int totalChunks) {
        rewardManager.givePersonalRewards(player);
        EffectManager.spawnFirework(player.getLocation());
        
        String message = ChatColor.GREEN + "" + ChatColor.BOLD + totalChunks + " チャンクを発見して報酬を受け取りました！";
        player.sendMessage(message);
    }

    /**
     * 個人マイルストーンをチェックして報酬を付与
     */
    private void checkPersonalMilestones(Player player, int totalChunks) {
        try {
            plugin.getLogger().info("個人マイルストーンチェック開始: プレイヤー=" + player.getName() + ", 総チャンク数=" + totalChunks);
            plugin.getLogger().info("利用可能なマイルストーン数: " + MilestoneConfig.personal.size());
            
            for (MilestoneConfig.MilestoneEntry milestone : MilestoneConfig.personal) {
                plugin.getLogger().info("マイルストーンチェック: 設定値=" + milestone.discoveryCount + ", 現在値=" + totalChunks);
                if (milestone.discoveryCount == totalChunks) {
                    // 報酬アイテムを付与
                    if (milestone.items != null) {
                        for (RewardItem rewardItem : milestone.items) {
                            rewardManager.giveRewardItem(player, rewardItem);
                        }
                    }

                    // メッセージ送信
                    if (milestone.sendMessage && milestone.message != null) {
                        if (milestone.broadcast) {
                            String broadcastMsg = ChatColor.YELLOW + "" + ChatColor.BOLD + "[マイルストーン達成] " +
                                    ChatColor.YELLOW + player.getName() + " さんが " + totalChunks + " チャンク発見を達成！";
                            Bukkit.getServer().broadcastMessage(broadcastMsg);
                        } else {
                            player.sendMessage(ChatColor.AQUA + milestone.message);
                        }
                    }

                    // エフェクト再生
                    if (milestone.playEffects) {
                        EffectManager.spawnFirework(player.getLocation());
                    }

                    plugin.getLogger().info(player.getName() + " が個人マイルストーン " + totalChunks + " チャンクを達成しました。");
                    break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("個人マイルストーンチェック中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * グローバルマイルストーンをチェックして報酬を付与
     */
    public void checkGlobalMilestones(int totalDiscoveredChunks) {
        try {
            plugin.getLogger().info("グローバルマイルストーンチェック開始: 総発見チャンク数=" + totalDiscoveredChunks);
            plugin.getLogger().info("利用可能なグローバルマイルストーン数: " + MilestoneConfig.global.size());
            plugin.getLogger().info("既にトリガーされたマイルストーン: " + triggeredGlobalMilestones);
            
            for (MilestoneConfig.MilestoneEntry milestone : MilestoneConfig.global) {
                plugin.getLogger().info("グローバルマイルストーンチェック: 設定値=" + milestone.discoveryCount + ", 現在値=" + totalDiscoveredChunks);
                if (milestone.discoveryCount == totalDiscoveredChunks &&
                        !triggeredGlobalMilestones.contains(milestone.discoveryCount)) {

                    // 一度だけ実行されるようにマーク
                    triggeredGlobalMilestones.add(milestone.discoveryCount);

                    // 全プレイヤーに報酬を配布
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            // 報酬アイテムを付与
                            if (milestone.items != null) {
                                for (RewardItem rewardItem : milestone.items) {
                                    rewardManager.giveRewardItem(onlinePlayer, rewardItem);
                                }
                            }
                        }

                        // グローバルメッセージ送信
                        if (milestone.sendMessage && milestone.message != null) {
                            String globalMsg = ChatColor.GOLD + "" + ChatColor.BOLD + "[グローバルマイルストーン達成] " +
                                    ChatColor.GOLD + "サーバー全体で " + totalDiscoveredChunks + " チャンクが発見されました！";
                            Bukkit.getServer().broadcastMessage(globalMsg);
                            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + milestone.message);
                        }

                        // 全プレイヤーにエフェクト
                        if (milestone.playEffects) {
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                EffectManager.spawnFirework(onlinePlayer.getLocation());
                            }
                        }
                    });

                    plugin.getLogger().info("グローバルマイルストーン " + totalDiscoveredChunks + " チャンクが達成されました。");
                    break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("グローバルマイルストーンチェック中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 報酬設定をリロード
     */
    public void reloadRewards() {
        try {
            rewardManager.loadMilestoneRewards();
            // グローバルマイルストーンのトリガー履歴をクリア（リロード時）
            triggeredGlobalMilestones.clear();
            plugin.getLogger().info("報酬設定がリロードされました。");
        } catch (Exception e) {
            plugin.getLogger().severe("報酬設定のリロード中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * グローバルマイルストーンのトリガー履歴をリセット（テスト用）
     */
    public void resetGlobalMilestoneHistory() {
        triggeredGlobalMilestones.clear();
        plugin.getLogger().info("グローバルマイルストーンの履歴がリセットされました。");
    }
}