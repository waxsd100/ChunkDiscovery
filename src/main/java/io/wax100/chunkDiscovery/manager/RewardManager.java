package io.wax100.chunkDiscovery.manager;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.model.RewardItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 報酬アイテムやマイルストーン設定を管理し、実際の付与を行うクラス
 */
public class RewardManager {
    private final ChunkDiscoveryPlugin plugin;
    private final Map<Integer, RewardItem> milestoneRewards = new ConcurrentHashMap<>();
    private RewardItem worldFirstReward;
    private RewardItem personalFirstReward;

    public RewardManager(ChunkDiscoveryPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 設定ファイルまたは初期値からマイルストーン報酬をロード
     */
    public void loadMilestoneRewards() {
        try {
            milestoneRewards.clear();

            // config.yml の milestones セクションを読み込む
            ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("rewards.milestones");
            if (milestonesSection != null) {
                for (String key : milestonesSection.getKeys(false)) {
                    try {
                        int milestone = Integer.parseInt(key);
                        ConfigurationSection rewardSection = milestonesSection.getConfigurationSection(key);
                        if (rewardSection != null) {
                            RewardItem rewardItem = RewardItem.fromConfig(rewardSection);
                            milestoneRewards.put(milestone, rewardItem);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("無効なマイルストーン値です: " + key);
                    }
                }
            }

            // 世界初/個人初も config から
            ConfigurationSection worldFirstSection = plugin.getConfig().getConfigurationSection("rewards.world_first");
            this.worldFirstReward = RewardItem.fromConfig(worldFirstSection);

            ConfigurationSection personalFirstSection = plugin.getConfig().getConfigurationSection("rewards.personal_first");
            this.personalFirstReward = RewardItem.fromConfig(personalFirstSection);

            plugin.getLogger().info("マイルストーン報酬を読み込みました。設定済み: " + milestoneRewards.size() + " 個");

        } catch (Exception e) {
            plugin.getLogger().severe("報酬設定の読み込み中にエラーが発生しました: " + e.getMessage());
            // デフォルト値で初期化
            initializeDefaultRewards();
        }
    }

    /**
     * デフォルト報酬の初期化
     */
    private void initializeDefaultRewards() {
        try {
            // デフォルトの世界初報酬
            this.worldFirstReward = new RewardItem(
                    new ItemStack(org.bukkit.Material.DIAMOND, 1),
                    100,
                    java.util.List.of()
            );

            // デフォルトの個人初報酬
            this.personalFirstReward = new RewardItem(
                    new ItemStack(org.bukkit.Material.BREAD, 5),
                    10,
                    java.util.List.of()
            );

            plugin.getLogger().info("デフォルト報酬設定を適用しました。");
        } catch (Exception e) {
            plugin.getLogger().severe("デフォルト報酬の初期化に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 世界初発見時の報酬を付与
     */
    public void giveWorldFirstRewards(Player player) {
        try {
            if (worldFirstReward != null) {
                giveRewardItem(player, worldFirstReward);
                player.sendMessage(Component.text("世界初発見報酬を受け取りました！").color(NamedTextColor.GOLD));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("世界初報酬付与中にエラーが発生しました: " + e.getMessage());
            player.sendMessage(Component.text("報酬の付与中にエラーが発生しました。").color(NamedTextColor.RED));
        }
    }

    /**
     * 個人初発見時の報酬を付与
     */
    public void givePersonalRewards(Player player) {
        try {
            if (personalFirstReward != null) {
                giveRewardItem(player, personalFirstReward);
                player.sendMessage(Component.text("個人初発見報酬を受け取りました！").color(NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("個人初報酬付与中にエラーが発生しました: " + e.getMessage());
            player.sendMessage(Component.text("報酬の付与中にエラーが発生しました。").color(NamedTextColor.RED));
        }
    }

    /**
     * マイルストーン到達時の報酬チェックおよび付与
     */
    public void checkMilestones(Player player, int totalChunks) {
        try {
            if (milestoneRewards.containsKey(totalChunks)) {
                RewardItem reward = milestoneRewards.get(totalChunks);
                giveRewardItem(player, reward);
                player.sendMessage(Component.text(totalChunks + " チャンク到達報酬を受け取りました！").color(NamedTextColor.AQUA));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("マイルストーン報酬付与中にエラーが発生しました: " + e.getMessage());
            player.sendMessage(Component.text("報酬の付与中にエラーが発生しました。").color(NamedTextColor.RED));
        }
    }

    /**
     * 報酬アイテムを実際にプレイヤーに付与する
     * @param player 対象プレイヤー
     * @param reward 付与する報酬
     */
    public void giveRewardItem(Player player, RewardItem reward) {
        try {
            if (reward == null) {
                return;
            }

            // アイテム付与
            if (reward.getItem() != null) {
                ItemStack item = reward.getItem().clone(); // アイテムをクローンして安全に付与

                // インベントリに空きがない場合の処理
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    player.sendMessage(Component.text("インベントリが満杯のため、アイテムを足元にドロップしました。").color(NamedTextColor.YELLOW));
                } else {
                    player.getInventory().addItem(item);
                }
            }

            // 経験値付与
            if (reward.getExperience() > 0) {
                player.giveExp(reward.getExperience());
            }

            // ポーション効果付与
            if (reward.getEffects() != null && !reward.getEffects().isEmpty()) {
                for (PotionEffect effect : reward.getEffects()) {
                    if (effect != null) {
                        // 既存の同じ効果がある場合は上書きしない（より強い効果を優先）
                        PotionEffect existingEffect = player.getPotionEffect(effect.getType());
                        if (existingEffect == null || existingEffect.getAmplifier() < effect.getAmplifier()) {
                            player.addPotionEffect(effect);
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("報酬アイテム付与中にエラーが発生しました: " + e.getMessage());
            player.sendMessage(Component.text("報酬の付与中にエラーが発生しました。").color(NamedTextColor.RED));
        }
    }

    /**
     * 設定されているマイルストーン一覧を取得
     */
    public Map<Integer, RewardItem> getMilestoneRewards() {
        return new ConcurrentHashMap<>(milestoneRewards);
    }

    /**
     * 特定のマイルストーンが設定されているかチェック
     */
    public boolean hasMilestoneReward(int chunks) {
        return milestoneRewards.containsKey(chunks);
    }
}