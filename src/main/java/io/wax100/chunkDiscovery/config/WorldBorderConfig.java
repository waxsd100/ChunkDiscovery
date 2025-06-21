package io.wax100.chunkDiscovery.config;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * ワールド別のボーダー設定を管理するクラス
 */
public class WorldBorderConfig {
    private static final Map<String, WorldBorderSetting> worldSettings = new HashMap<>();
    private static WorldBorderSetting defaultSetting;
    private static ChunkDiscoveryPlugin plugin;

    public static void init(ChunkDiscoveryPlugin pluginInstance) {
        plugin = pluginInstance;
        loadSettings();
        applyInitialBorders();
    }

    private static void loadSettings() {
        worldSettings.clear();

        // デフォルト設定を読み込み
        double defaultInitSize = plugin.getConfig().getDouble("border.initial_size", 100.0);
        double defaultPerChunk = plugin.getConfig().getDouble("border.expansion_per_chunk", 1.0);
        defaultSetting = new WorldBorderSetting(defaultInitSize, defaultPerChunk);

        // ワールド別設定を読み込み
        ConfigurationSection worldSection = plugin.getConfig().getConfigurationSection("border.worlds");
        if (worldSection != null) {
            for (String worldName : worldSection.getKeys(false)) {
                ConfigurationSection settings = worldSection.getConfigurationSection(worldName);
                if (settings != null) {
                    double initSize = settings.getDouble("initial_size", defaultInitSize);
                    double perChunk = settings.getDouble("expansion_per_chunk", defaultPerChunk);
                    worldSettings.put(worldName, new WorldBorderSetting(initSize, perChunk));
                }
            }
        }

        // ワールドタイプ別設定を読み込み
        ConfigurationSection typeSection = plugin.getConfig().getConfigurationSection("border.world_types");
        if (typeSection != null) {
            for (String typeName : typeSection.getKeys(false)) {
                ConfigurationSection settings = typeSection.getConfigurationSection(typeName);
                if (settings != null) {
                    double initSize = settings.getDouble("initial_size", defaultInitSize);
                    double perChunk = settings.getDouble("expansion_per_chunk", defaultPerChunk);

                    // 該当するワールドタイプの全ワールドに適用
                    for (World world : plugin.getServer().getWorlds()) {
                        if (matchesWorldType(world, typeName)) {
                            // 個別設定がない場合のみタイプ設定を適用
                            if (!worldSettings.containsKey(world.getName())) {
                                worldSettings.put(world.getName(), new WorldBorderSetting(initSize, perChunk));
                            }
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("ワールドボーダー設定を読み込みました。設定済みワールド数: " + worldSettings.size());
    }

    private static boolean matchesWorldType(World world, String typeName) {
        return switch (typeName.toLowerCase()) {
            case "overworld", "normal" -> world.getEnvironment() == World.Environment.NORMAL;
            case "nether" -> world.getEnvironment() == World.Environment.NETHER;
            case "end", "the_end" -> world.getEnvironment() == World.Environment.THE_END;
            default -> false;
        };
    }

    private static void applyInitialBorders() {
        for (World world : plugin.getServer().getWorlds()) {
            WorldBorderSetting setting = getSettingForWorld(world.getName());
            world.getWorldBorder().setSize(setting.initialSize());
            plugin.getLogger().info("ワールド " + world.getName() + " のボーダーサイズを " + setting.initialSize() + " に設定しました。");
        }
    }

    public static double calculateNewSize(World world, int totalChunks) {
        WorldBorderSetting setting = getSettingForWorld(world.getName());
        return setting.initialSize() + (totalChunks * setting.expansionPerChunk());
    }

    public static WorldBorderSetting getSettingForWorld(String worldName) {
        return worldSettings.getOrDefault(worldName, defaultSetting);
    }

    public static void reloadSettings() {
        loadSettings();
        applyInitialBorders();
    }

    /**
     * ワールドボーダー設定を表すレコードクラス
     */
    public record WorldBorderSetting(double initialSize, double expansionPerChunk) {
        public WorldBorderSetting {
            if (initialSize < 0 || initialSize > 60000000) {
                throw new IllegalArgumentException("初期サイズは0以上60000000以下である必要があります: " + initialSize);
            }
            if (expansionPerChunk < 0 || expansionPerChunk > 1000) {
                throw new IllegalArgumentException("チャンクあたりの拡張量は0以上1000以下である必要があります: " + expansionPerChunk);
            }
        }
    }
}