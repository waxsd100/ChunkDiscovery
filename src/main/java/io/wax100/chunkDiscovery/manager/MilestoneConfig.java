package io.wax100.chunkDiscovery.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import io.wax100.chunkDiscovery.model.RewardItem;
import java.util.*;

public class MilestoneConfig {
    public static List<MilestoneEntry> personal = new ArrayList<>();
    public static List<MilestoneEntry> global = new ArrayList<>();

    public static void init(JavaPlugin plugin) {
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("rewards.milestones");
        if (milestonesSection != null) {
            personal.clear();
            for (String key : milestonesSection.getKeys(false)) {
                try {
                    int discoveryCount = Integer.parseInt(key);
                    ConfigurationSection milestoneSection = milestonesSection.getConfigurationSection(key);
                    if (milestoneSection != null) {
                        MilestoneEntry entry = createMilestoneEntry(discoveryCount, milestoneSection);
                        personal.add(entry);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid milestone key: " + key);
                }
            }
            personal.sort(Comparator.comparingInt(entry -> entry.discoveryCount));
        }
        
        // グローバルマイルストーンは今のところ個人マイルストーンと同じ設定を使用
        global = new ArrayList<>(personal);
    }

    private static MilestoneEntry createMilestoneEntry(int discoveryCount, ConfigurationSection section) {
        MilestoneEntry entry = new MilestoneEntry();
        entry.discoveryCount = discoveryCount;
        entry.items = new ArrayList<>();
        entry.sendMessage = true;
        entry.broadcast = discoveryCount >= 100; // 100以上は全体通知
        entry.playEffects = true;
        entry.message = discoveryCount + "チャンク発見達成！";

        // RewardItem.fromConfigを使用して報酬を作成
        RewardItem rewardItem = RewardItem.fromConfig(section);
        entry.items.add(rewardItem);

        return entry;
    }

    public static class MilestoneEntry {
        public int discoveryCount;
        public List<RewardItem> items;
        public String message;
        public boolean sendMessage = true;
        public boolean broadcast = false;
        public boolean playEffects = false;
    }
}