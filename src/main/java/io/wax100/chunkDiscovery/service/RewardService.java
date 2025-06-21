package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.manager.EffectManager;
import io.wax100.chunkDiscovery.manager.RewardManager;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.model.RewardItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 報酬配布を統括するサービスクラス（グローバルマイルストーン対応）
 */
public class RewardService {
    private final ChunkDiscoveryPlugin plugin;
    private final RewardManager rewardManager;
    private final Set<Integer> triggeredGlobalMilestones = ConcurrentHashMap.newKeySet();

    public RewardService(ChunkDiscoveryPlugin plugin) {
        this.plugin = plugin;
        this.rewardManager = new RewardManager(plugin);
        try {
            this.rewardManager.loadMilestoneRewards();
        } catch (Exception e) {
            plugin.getLogger().warning("報酬設定の読み込みに失敗しました: " + e.getMessage());
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (globalFirst) {
                    rewardManager.giveWorldFirstRewards(player);
                    Component message = Component.text("[世界初発見] ")
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD)
                            .append(Component.text(player.getName() + " さんが " + totalChunks + " チャンクを世界初発見！")
                                    .color(NamedTextColor.GOLD));
                    plugin.getServer().broadcast(message);
                    EffectManager.spawnFirework(player.getLocation());
                } else if (personalFirst) {
                    rewardManager.givePersonalRewards(player);
                    Component message = Component.text("[個人初発見] ")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .append(Component.text(totalChunks + " チャンク目を発見して報酬を受け取りました！")
                                    .color(NamedTextColor.GREEN));
                    player.sendMessage(message);
                    EffectManager.spawnFirework(player.getLocation());
                }

                // 個人マイルストーンチェック
                checkPersonalMilestones(player, totalChunks);

            } catch (Exception e) {
                plugin.getLogger().severe("報酬付与中にエラーが発生しました: " + e.getMessage());
                player.sendMessage("§c報酬の付与中にエラーが発生しました。");
            }
        });
    }

    /**
     * 個人マイルストーンをチェックして報酬を付与
     */
    private void checkPersonalMilestones(Player player, int totalChunks) {
        try {
            for (MilestoneConfig.MilestoneEntry milestone : MilestoneConfig.personal) {
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
                            Component broadcastMsg = Component.text("[マイルストーン達成] ")
                                    .color(NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.BOLD)
                                    .append(Component.text(player.getName() + " さんが " + totalChunks + " チャンク発見を達成！")
                                            .color(NamedTextColor.YELLOW));
                            plugin.getServer().broadcast(broadcastMsg);
                        } else {
                            player.sendMessage(Component.text(milestone.message).color(NamedTextColor.AQUA));
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
            for (MilestoneConfig.MilestoneEntry milestone : MilestoneConfig.global) {
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
                            Component globalMsg = Component.text("[グローバルマイルストーン達成] ")
                                    .color(NamedTextColor.GOLD)
                                    .decorate(TextDecoration.BOLD)
                                    .append(Component.text("サーバー全体で " + totalDiscoveredChunks + " チャンクが発見されました！")
                                            .color(NamedTextColor.GOLD));
                            plugin.getServer().broadcast(globalMsg);
                            plugin.getServer().broadcast(Component.text(milestone.message).color(NamedTextColor.YELLOW));
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