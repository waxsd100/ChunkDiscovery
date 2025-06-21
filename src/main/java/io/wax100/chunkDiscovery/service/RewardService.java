package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.manager.EffectManager;
import io.wax100.chunkDiscovery.manager.RewardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 報酬配布を統括するサービスクラス
 */
public class RewardService {
    private final ChunkDiscoveryPlugin plugin;
    private final RewardManager rewardManager;

    public RewardService(ChunkDiscoveryPlugin plugin) {
        this.plugin = plugin;
        this.rewardManager = new RewardManager(plugin);
        this.rewardManager.loadMilestoneRewards();
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
            rewardManager.checkMilestones(player, totalChunks);
        });
    }


}
