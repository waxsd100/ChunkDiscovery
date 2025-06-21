package io.wax100.chunkDiscovery.manager;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.model.RewardItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 報酬アイテムやマイルストーン設定を管理し、実際の付与を行うクラス
 */
public class RewardManager {
    private final ChunkDiscoveryPlugin plugin;
    private Map<Integer, RewardItem> milestoneRewards;
    private RewardItem worldFirstReward;
    private RewardItem personalFirstReward;

    public RewardManager(ChunkDiscoveryPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 設定ファイルまたは初期値からマイルストーン報酬をロード
     */
    public void loadMilestoneRewards() {
        // config.yml の milestones セクションを読み込む例
        this.milestoneRewards = plugin.getConfig().getConfigurationSection("rewards.milestones").getKeys(false).stream()
                .map(Integer::valueOf)
                .collect(Collectors.toMap(
                        key -> key,
                        key -> RewardItem.fromConfig(plugin.getConfig().getConfigurationSection("rewards.milestones." + key))
                ));

        // 世界初/個人初も config から
        this.worldFirstReward = RewardItem.fromConfig(plugin.getConfig().getConfigurationSection("rewards.world_first"));
        this.personalFirstReward = RewardItem.fromConfig(plugin.getConfig().getConfigurationSection("rewards.personal_first"));
    }

    /**
     * 世界初発見時の報酬を付与
     */
    public void giveWorldFirstRewards(Player player) {
        giveItem(player, worldFirstReward);
        player.sendMessage(Component.text("世界初発見報酬を受け取りました！").color(NamedTextColor.GOLD));
    }

    /**
     * 個人初発見時の報酬を付与
     */
    public void givePersonalRewards(Player player) {
        giveItem(player, personalFirstReward);
        player.sendMessage(Component.text("個人初発見報酬を受け取りました！").color(NamedTextColor.GREEN));
    }

    /**
     * マイルストーン到達時の報酬チェックおよび付与
     */
    public void checkMilestones(Player player, int totalChunks) {
        if (milestoneRewards.containsKey(totalChunks)) {
            RewardItem reward = milestoneRewards.get(totalChunks);
            giveItem(player, reward);
            player.sendMessage(Component.text(totalChunks + " チャンク到達報酬を受け取りました！").color(NamedTextColor.AQUA));
        }
    }

    private void giveItem(Player player, RewardItem reward) {
        if (reward.getItem() != null) {
            player.getInventory().addItem(reward.getItem());
        }
        if (reward.getExperience() > 0) {
            player.giveExp(reward.getExperience());
        }
        // ポーション効果などもあれば
        for (PotionEffect eff : reward.getEffects()) {
            player.addPotionEffect(eff);
        }
    }
}
