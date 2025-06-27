package io.wax100.chunkDiscovery.config;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.WorldBorderRepository;
import io.wax100.chunkDiscovery.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.World;
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

                if (savedSize != null) {
                    // 保存されたサイズがある場合は復元
                    world.getWorldBorder().setSize(savedSize);
                    plugin.getLogger().info("ワールド " + worldName + " のボーダーサイズを復元しました: " + savedSize);
                } else {
                    // 保存されたサイズがない場合は初期サイズを設定してDBに保存
                    WorldBorderSetting setting = getSettingForWorld(worldName);
                    world.getWorldBorder().setSize(setting.initialSize());
                    borderRepository.initializeBorderIfAbsent(worldName, setting.initialSize());
                    plugin.getLogger().info("ワールド " + worldName + " のボーダーサイズを初期化しました: " + setting.initialSize());
                }
                
                // スポーン地点をWorldボーダーの中心に設定
                setSpawnToBorderCenter(world);
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
            WorldBorderSetting setting = getSettingForWorld(world.getName());
            world.getWorldBorder().setSize(setting.initialSize());
            plugin.getLogger().info("ワールド " + world.getName() + " のボーダーサイズを設定ファイルから設定しました: " + setting.initialSize());
            
            // スポーン地点をWorldボーダーの中心に設定
            setSpawnToBorderCenter(world);
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
     * スポーン地点をWorldボーダーの中心に設定
     * @param world 対象ワールド
     */
    private static void setSpawnToBorderCenter(World world) {
        // プラグインの状態チェック
        if (plugin == null || !plugin.isEnabled()) {
            if (plugin != null) {
                plugin.getLogger().warning("プラグインが無効化されているため、スポーン地点設定をスキップします: " + world.getName());
            }
            return;
        }
        
        try {
            // Worldボーダーの中心を(0, 0)に固定設定
            org.bukkit.WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(0, 0);
            
            // 設定値を事前に取得（メインスレッドで実行）
            final int searchRadius = plugin.getConfig().getInt("spawn.search_radius", 8);
            final int netherMinY = plugin.getConfig().getInt("spawn.nether.min_y", 64);
            final int netherMaxY = plugin.getConfig().getInt("spawn.nether.max_y", 96);
            
            // 各ワールドタイプに応じたスポーン地点設定を非同期で実行
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Location spawnLocation = determineSpawnLocation(world, searchRadius, netherMinY, netherMaxY);
                    
                    // プラグインの状態を再チェック
                    if (plugin == null || !plugin.isEnabled()) {
                        return;
                    }
                    
                    // メインスレッドでスポーン地点を設定
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        try {
                            // 最終的なプラグイン状態チェック
                            if (plugin == null || !plugin.isEnabled()) {
                                return;
                            }
                            
                            world.setSpawnLocation(spawnLocation);
                            plugin.getLogger().info(String.format("%s %s のスポーン地点を設定しました: %.1f, %.1f, %.1f", 
                                getWorldTypeName(world), world.getName(), 
                                spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ()));
                        } catch (Exception e) {
                            if (plugin != null) {
                                plugin.getLogger().severe("スポーン地点設定中にエラーが発生しました (" + world.getName() + "): " + e.getMessage());
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    if (plugin != null) {
                        plugin.getLogger().severe("スポーン地点検索中にエラーが発生しました (" + world.getName() + "): " + e.getMessage());
                        // フォールバック: メインスレッドでデフォルト座標を設定
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            try {
                                if (plugin == null || !plugin.isEnabled()) {
                                    return;
                                }
                                Location fallback = new Location(world, 0, 64, 0);
                                world.setSpawnLocation(fallback);
                                plugin.getLogger().warning(world.getName() + " でエラー発生のため、デフォルト座標(0, 64, 0)を使用します");
                            } catch (Exception ex) {
                                // 最終的なエラーハンドリング
                            }
                        });
                    }
                }
            });
            
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().severe("Worldボーダー設定中にエラーが発生しました (" + world.getName() + "): " + e.getMessage());
            }
        }
    }
    
    /**
     * ワールドタイプ名を取得
     * @param world 対象ワールド
     * @return ワールドタイプ名
     */
    private static String getWorldTypeName(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> "オーバーワールド";
            case NETHER -> "Nether";
            case THE_END -> "End";
            case CUSTOM -> "カスタム";
        };
    }
    
    /**
     * ワールドタイプに応じてスポーン地点を決定
     * @param world 対象ワールド
     * @param searchRadius 検索半径
     * @param netherMinY Netherの最小Y座標
     * @param netherMaxY Netherの最大Y座標
     * @return 安全なスポーン地点
     */
    private static Location determineSpawnLocation(World world, int searchRadius, int netherMinY, int netherMaxY) {
        return switch (world.getEnvironment()) {
            case NORMAL -> findSafeOverworldSpawn(world, searchRadius);
            case NETHER -> findSafeNetherSpawn(world, searchRadius, netherMinY, netherMaxY);
            case THE_END -> new Location(world, 0, 64, 0); // Endは固定
            case CUSTOM -> findSafeOverworldSpawn(world, searchRadius); // カスタムワールドはオーバーワールド扱い
        };
    }
    
    /**
     * オーバーワールドで安全なスポーン地点を検索
     * @param world 対象ワールド
     * @param maxRadius 最大検索半径
     * @return 安全なスポーン地点
     */
    private static Location findSafeOverworldSpawn(World world, int maxRadius) {
        // 中心(0,0)から螺旋状に安全な場所を検索
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && radius > 0) continue; // 中心は最初にチェック済み
                    
                    try {
                        // チャンクを安全にロード
                        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                            world.loadChunk(x >> 4, z >> 4);
                        }
                        
                        int safeY = findSafeYCoordinate(world, x, z);
                        if (safeY > 0) {
                            return new Location(world, x + 0.5, safeY, z + 0.5);
                        }
                    } catch (Exception e) {
                        // ログ出力時のnullチェック
                        if (plugin != null) {
                            plugin.getLogger().fine("座標チェック中にエラー (" + x + ", " + z + "): " + e.getMessage());
                        }
                    }
                }
            }
        }
        // フォールバック: 海面レベルの中心
        return new Location(world, 0.5, 64, 0.5);
    }
    
    /**
     * オーバーワールドで安全なY座標を検索
     * @param world 対象ワールド
     * @param x X座標
     * @param z Z座標
     * @return 安全なY座標（見つからない場合は-1）
     */
    private static int findSafeYCoordinate(World world, int x, int z) {
        // 地上の安全な座標を検索（Y=320から下に向かって）
        for (int y = 320; y >= -64; y--) {
            Location checkLocation = new Location(world, x, y, z);
            if (isSafeOverworldLocation(world, checkLocation)) {
                return y + 1; // 1ブロック上に設定
            }
        }
        return -1; // 見つからない
    }
    
    /**
     * オーバーワールドの座標が安全かチェック
     * @param world 対象ワールド
     * @param location チェック座標
     * @return 安全かどうか
     */
    private static boolean isSafeOverworldLocation(World world, Location location) {
        try {
            org.bukkit.block.Block block = world.getBlockAt(location);
            org.bukkit.block.Block above = world.getBlockAt(location.clone().add(0, 1, 0));
            org.bukkit.block.Block above2 = world.getBlockAt(location.clone().add(0, 2, 0));
            
            // 足元が固体で危険ブロックでない
            if (!block.getType().isSolid() || isDangerousBlock(block.getType())) {
                return false;
            }
            
            // 頭上2ブロックが空気または安全な植物
            return (above.getType().isAir() || isSafePlant(above.getType())) &&
                   (above2.getType().isAir() || isSafePlant(above2.getType()));
                   
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 危険なブロックかチェック
     * @param blockType ブロックタイプ
     * @return 危険かどうか
     */
    private static boolean isDangerousBlock(org.bukkit.Material blockType) {
        String name = blockType.name();
        return name.contains("LAVA") || name.contains("FIRE") || 
               name.contains("MAGMA") || name.equals("CACTUS");
    }
    
    /**
     * 安全な植物かチェック
     * @param blockType ブロックタイプ
     * @return 安全な植物かどうか
     */
    private static boolean isSafePlant(org.bukkit.Material blockType) {
        String name = blockType.name();
        return name.contains("GRASS") || name.contains("FLOWER") || 
               name.contains("SAPLING") || name.equals("FERN");
    }
    
    /**
     * Netherで安全なスポーン地点を検索
     * @param world 対象ワールド
     * @param maxRadius 最大検索半径
     * @param minY 最小Y座標
     * @param maxY 最大Y座標
     * @return 安全なスポーン地点
     */
    private static Location findSafeNetherSpawn(World world, int maxRadius, int minY, int maxY) {
        
        // 中心(0,0)から螺旋状に安全な場所を検索
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && radius > 0) continue;
                    
                    try {
                        // チャンクを安全にロード
                        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                            world.loadChunk(x >> 4, z >> 4);
                        }
                        
                        // Y座標範囲で安全な場所を検索
                        for (int y = minY; y <= maxY; y++) {
                            Location checkLocation = new Location(world, x + 0.5, y, z + 0.5);
                            if (isSafeNetherLocation(world, checkLocation)) {
                                return checkLocation;
                            }
                        }
                    } catch (Exception e) {
                        // ログ出力時のnullチェック
                        if (plugin != null) {
                            plugin.getLogger().fine("Nether座標チェック中にエラー (" + x + ", " + z + "): " + e.getMessage());
                        }
                    }
                }
            }
        }
        // フォールバック: デフォルト高度の中心
        return new Location(world, 0.5, 64, 0.5);
    }
    
    /**
     * Netherの座標が安全かチェック
     * @param world 対象ワールド
     * @param location チェック座標
     * @return 安全かどうか
     */
    private static boolean isSafeNetherLocation(World world, Location location) {
        try {
            org.bukkit.block.Block block = world.getBlockAt(location);
            org.bukkit.block.Block above = world.getBlockAt(location.clone().add(0, 1, 0));
            org.bukkit.block.Block above2 = world.getBlockAt(location.clone().add(0, 2, 0));
            
            // 足元が固体で危険ブロックでない
            if (!block.getType().isSolid() || isDangerousBlock(block.getType())) {
                return false;
            }
            
            // 頭上2ブロックが空気
            if (!above.getType().isAir() || !above2.getType().isAir()) {
                return false;
            }
            
            // 近くに溶岩がない
            return !isNearLava(world, location);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 溶岩が近くにあるかチェック
     * @param world 対象ワールド
     * @param location チェック座標
     * @return 溶岩が近くにある場合true
     */
    private static boolean isNearLava(World world, Location location) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Location checkLoc = location.clone().add(dx, dy, dz);
                    if (world.getBlockAt(checkLoc).getType().name().contains("LAVA")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 設定とDBからのボーダー復元
     */
    public static void reloadSettings() {
        loadSettings();
        applyBordersFromDatabase();
    }
    
    /**
     * プラグイン無効化時のクリーンアップ
     */
    public static void cleanup() {
        worldSettings.clear();
        defaultSetting = null;
        plugin = null;
        borderRepository = null;
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