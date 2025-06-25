package io.wax100.chunkDiscovery.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import io.wax100.chunkDiscovery.model.RewardItem;
import java.util.*;
import java.util.logging.Logger;

public class MilestoneConfig {
    private final List<MilestoneEntry> personalMilestones = new ArrayList<>();
    private final List<MilestoneEntry> globalMilestones = new ArrayList<>();
    private final Logger logger;

    public MilestoneConfig(Logger logger) {
        this.logger = logger;
    }

    public void loadConfiguration(ConfigurationSection config) {
        ConfigurationSection milestonesSection = config.getConfigurationSection("rewards.milestones");
        if (milestonesSection == null) {
            logger.warning("マイルストーン設定が見つかりません");
            return;
        }

        personalMilestones.clear();
        globalMilestones.clear();

        loadMilestones(milestonesSection);
        
        // グローバルマイルストーンは今のところ個人マイルストーンと同じ設定を使用
        globalMilestones.addAll(personalMilestones);
        
        logger.info(String.format("マイルストーン設定を読み込みました: %d個", personalMilestones.size()));
    }

    private void loadMilestones(ConfigurationSection milestonesSection) {
        for (String key : milestonesSection.getKeys(false)) {
            try {
                int discoveryCount = Integer.parseInt(key);
                ConfigurationSection milestoneSection = milestonesSection.getConfigurationSection(key);
                
                if (milestoneSection != null) {
                    MilestoneEntry entry = createMilestoneEntry(discoveryCount, milestoneSection);
                    personalMilestones.add(entry);
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid milestone key: " + key);
            }
        }
        
        personalMilestones.sort(Comparator.comparingInt(entry -> entry.discoveryCount));
    }

    private MilestoneEntry createMilestoneEntry(int discoveryCount, ConfigurationSection section) {
        MilestoneEntry entry = new MilestoneEntry();
        entry.discoveryCount = discoveryCount;
        entry.items = new ArrayList<>();
        entry.sendMessage = section.getBoolean("send_message", true);
        entry.broadcast = section.getBoolean("broadcast", discoveryCount >= 100);
        entry.playEffects = section.getBoolean("play_effects", true);
        entry.message = section.getString("message", discoveryCount + "チャンク発見達成！");

        try {
            RewardItem rewardItem = RewardItem.fromConfig(section);
            entry.items.add(rewardItem);
        } catch (Exception e) {
            logger.warning(String.format("マイルストーン %d の報酬設定に問題があります: %s", discoveryCount, e.getMessage()));
        }

        return entry;
    }

    public List<MilestoneEntry> getPersonalMilestones() {
        return Collections.unmodifiableList(personalMilestones);
    }

    public List<MilestoneEntry> getGlobalMilestones() {
        return Collections.unmodifiableList(globalMilestones);
    }

    public Optional<MilestoneEntry> getMilestone(int discoveryCount, boolean isGlobal) {
        List<MilestoneEntry> milestones = isGlobal ? globalMilestones : personalMilestones;
        return milestones.stream()
            .filter(entry -> entry.discoveryCount == discoveryCount)
            .findFirst();
    }

    public static class MilestoneEntry {
        public int discoveryCount;
        public List<RewardItem> items;
        public String message;
        public boolean sendMessage = true;
        public boolean broadcast = false;
        public boolean playEffects = false;
    }

    // レガシーサポート用の静的メソッド
    private static MilestoneConfig instance;
    
    public static void setInstance(MilestoneConfig config) {
        instance = config;
    }
    
    @Deprecated
    public static void init(JavaPlugin plugin) {
        if (instance != null) {
            instance.loadConfiguration(plugin.getConfig());
        }
    }
    
    @Deprecated
    public static List<MilestoneEntry> personal = new ArrayList<MilestoneEntry>() {
        @Override
        public List<MilestoneEntry> subList(int fromIndex, int toIndex) {
            return instance != null ? instance.getPersonalMilestones().subList(fromIndex, toIndex) : super.subList(fromIndex, toIndex);
        }
        
        @Override
        public int size() {
            return instance != null ? instance.getPersonalMilestones().size() : super.size();
        }
        
        @Override
        public MilestoneEntry get(int index) {
            return instance != null ? instance.getPersonalMilestones().get(index) : super.get(index);
        }
        
        @Override
        public java.util.Iterator<MilestoneEntry> iterator() {
            return instance != null ? instance.getPersonalMilestones().iterator() : super.iterator();
        }
        
        @Override
        public boolean isEmpty() {
            return instance != null ? instance.getPersonalMilestones().isEmpty() : super.isEmpty();
        }
    };
    
    @Deprecated  
    public static List<MilestoneEntry> global = new ArrayList<MilestoneEntry>() {
        @Override
        public List<MilestoneEntry> subList(int fromIndex, int toIndex) {
            return instance != null ? instance.getGlobalMilestones().subList(fromIndex, toIndex) : super.subList(fromIndex, toIndex);
        }
        
        @Override
        public int size() {
            return instance != null ? instance.getGlobalMilestones().size() : super.size();
        }
        
        @Override
        public MilestoneEntry get(int index) {
            return instance != null ? instance.getGlobalMilestones().get(index) : super.get(index);
        }
        
        @Override
        public java.util.Iterator<MilestoneEntry> iterator() {
            return instance != null ? instance.getGlobalMilestones().iterator() : super.iterator();
        }
        
        @Override
        public boolean isEmpty() {
            return instance != null ? instance.getGlobalMilestones().isEmpty() : super.isEmpty();
        }
    };
}