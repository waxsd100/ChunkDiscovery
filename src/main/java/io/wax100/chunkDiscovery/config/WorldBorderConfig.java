package io.wax100.chunkDiscovery.config;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.WorldBorderRepository;
import io.wax100.chunkDiscovery.database.DatabaseManager;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * ワールド別のボーダー設定を管理するクラス（DB永続化対応）
 */
public class WorldBorderConfig {
    private static final Map<String, WorldBorderSetting> worldSettings = new HashMap<>();
    private static WorldBorderSetting defaultSetting;
    private static ChunkDiscoveryPlugin plugin;
    private static WorldBorderRepository borderRepository;

    public static void init(ChunkDiscoveryPlugin pluginInstance) {
        plugin = pluginInstance;
        borderRepository = new WorldBorderRepository(DatabaseManager.getDataSource());
        loadSettings();
        applyBordersFromDatabase();
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

    /**
     * データベースから保存されたボーダーサイズを復元
     */
    private static void applyBordersFromDatabase() {
        try {
            Map<String, Double> savedBorders = borderRepository.getAllBorderSizes();

            for (World world : plugin.getServer().getWorlds()) {
                String worldName = world.getName();
                Double savedSize = savedBorders.get(worldName);

                // スポーン地点を取得してボーダーの中心に設定
                Location spawnLocation = world.getSpawnLocation();
                world.getWorldBorder().setCenter(spawnLocation.getX(), spawnLocation.getZ());
                
                if (savedSize != null) {
                    // 保存されたサイズがある場合は復元
                    world.getWorldBorder().setSize(savedSize);
                    plugin.getLogger().info("ワールド " + worldName + " のボーダーサイズを復元しました: " + savedSize + " (中心: " + spawnLocation.getX() + ", " + spawnLocation.getZ() + ")");
                } else {
                    // 保存されたサイズがない場合は初期サイズを設定してDBに保存
                    WorldBorderSetting setting = getSettingForWorld(worldName);
                    world.getWorldBorder().setSize(setting.initialSize());
                    borderRepository.initializeBorderIfAbsent(worldName, setting.initialSize());
                    plugin.getLogger().info("ワールド " + worldName + " のボーダーサイズを初期化しました: " + setting.initialSize() + " (中心: " + spawnLocation.getX() + ", " + spawnLocation.getZ() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("データベースからのボーダーサイズ復元中にエラーが発生しました: " + e.getMessage());
            // エラー時は設定ファイルの初期値を適用
            applyInitialBorders();
        }
    }

    /**
     * 設定ファイルの初期値でボーダーを設定（フォールバック用）
     */
    private static void applyInitialBorders() {
        for (World world : plugin.getServer().getWorlds()) {
            // スポーン地点を取得してボーダーの中心に設定
            Location spawnLocation = world.getSpawnLocation();
            world.getWorldBorder().setCenter(spawnLocation.getX(), spawnLocation.getZ());
            
            WorldBorderSetting setting = getSettingForWorld(world.getName());
            world.getWorldBorder().setSize(setting.initialSize());
            plugin.getLogger().info("ワールド " + world.getName() + " のボーダーサイズを設定ファイルから設定しました: " + setting.initialSize() + " (中心: " + spawnLocation.getX() + ", " + spawnLocation.getZ() + ")");
        }
    }

    /**
     * チャンク発見に応じて新しいボーダーサイズを計算
     * @param world 対象ワールド
     * @param totalChunks そのワールドでの発見済みチャンク総数
     * @return 新しいボーダーサイズ
     */
    public static double calculateNewSize(World world, int totalChunks) {
        WorldBorderSetting setting = getSettingForWorld(world.getName());
        return setting.initialSize() + (totalChunks * setting.expansionPerChunk());
    }

    /**
     * ワールドボーダーサイズを更新してDBに保存
     * @param world 対象ワールド
     * @param newSize 新しいボーダーサイズ
     * @param totalChunks 発見済みチャンク総数
     */
    public static void updateBorderSize(World world, double newSize, int totalChunks) {
        try {
            // ワールドボーダーを更新
            world.getWorldBorder().setSize(newSize);

            // データベースに保存
            borderRepository.saveBorderSize(world.getName(), newSize, totalChunks);

            plugin.getLogger().fine("ワールド " + world.getName() + " のボーダーサイズを更新しました: " + newSize + " (チャンク数: " + totalChunks + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("ボーダーサイズ更新中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * ワールドの現在のボーダーサイズをDBから取得
     * @param worldName ワールド名
     * @return 保存されたボーダーサイズ（存在しない場合は設定ファイルの初期値）
     */
    public static double getCurrentBorderSize(String worldName) {
        try {
            Double savedSize = borderRepository.getBorderSize(worldName);
            if (savedSize != null) {
                return savedSize;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("ボーダーサイズ取得中にエラーが発生しました: " + e.getMessage());
        }

        // フォールバック：設定ファイルの初期値を返す
        WorldBorderSetting setting = getSettingForWorld(worldName);
        return setting.initialSize();
    }

    public static WorldBorderSetting getSettingForWorld(String worldName) {
        return worldSettings.getOrDefault(worldName, defaultSetting);
    }

    /**
     * 設定とDBからのボーダー復元
     */
    public static void reloadSettings() {
        loadSettings();
        applyBordersFromDatabase();
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