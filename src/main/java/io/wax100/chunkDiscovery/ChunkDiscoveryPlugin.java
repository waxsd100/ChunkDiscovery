package io.wax100.chunkDiscovery;

import com.google.gson.Gson;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.database.DatabaseManager;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.service.RewardService;
import io.wax100.chunkDiscovery.manager.EffectManager;
import io.wax100.chunkDiscovery.manager.RewardManager;
import io.wax100.chunkDiscovery.listener.ChunkDiscoveryListener;
import io.wax100.chunkDiscovery.commands.ChunkDiscoveryCommand;
import io.wax100.chunkDiscovery.util.ChunkValidationCache;
import io.wax100.chunkDiscovery.config.WorldBorderConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

public class ChunkDiscoveryPlugin extends JavaPlugin {

    private DiscoveryService discoveryService;
    private RewardService rewardService;
    private ChunkValidationCache chunkCache;

    @Override
    public void onEnable() {
        try {
            // config.yml をコピー
            saveDefaultConfig();

            // 設定値のバリデーション
            if (!validateConfig()) {
                getLogger().severe("設定ファイルに無効な値が含まれています。プラグインを無効化します。");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // JSON をコピー
            saveResource("milestones.json", false);
            saveResource("global_milestones.json", false);

            // ワールドボーダー設定の初期化
            WorldBorderConfig.init(this);

            // DB 初期化（テーブル生成も含む）
            try {
                DatabaseManager.init(
                        getConfig().getString("db.host"),
                        getConfig().getInt("db.port"),
                        getConfig().getString("db.name"),
                        getConfig().getString("db.user"),
                        getConfig().getString("db.pass")
                );
                getLogger().info("データベース接続が確立されました。");
            } catch (Exception e) {
                getLogger().severe("データベース初期化に失敗しました: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // マイルストーン JSON をロード
            try {
                MilestoneConfig.init(getDataFolder().toPath());
                getLogger().info("マイルストーン設定を読み込みました。");
            } catch (Exception e) {
                getLogger().warning("マイルストーン設定の読み込みに失敗しました: " + e.getMessage());
            }

            // キャッシュ初期化
            chunkCache = new ChunkValidationCache();

            // サービス初期化
            rewardService = new RewardService(this);
            discoveryService = new DiscoveryService(
                    new PlayerRepository(DatabaseManager.getDataSource()),
                    new ChunkRepository(DatabaseManager.getDataSource()),
                    rewardService,
                    chunkCache,
                    this
            );

            // イベント/コマンド登録
            getServer().getPluginManager().registerEvents(
                    new ChunkDiscoveryListener(discoveryService), this
            );
            getCommand("chunkdiscovery").setExecutor(
                    new ChunkDiscoveryCommand(discoveryService, this)
            );

            getLogger().info("ChunkDiscoveryPlugin が正常に有効化されました。");

        } catch (Exception e) {
            getLogger().severe("プラグインの初期化中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (chunkCache != null) {
                chunkCache.clearCache();
            }
            DatabaseManager.shutdown();
            getLogger().info("ChunkDiscoveryPlugin が正常に無効化されました。");
        } catch (Exception e) {
            getLogger().severe("プラグインの無効化中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 設定ファイルをリロードする
     */
    public void reloadPluginConfig() {
        try {
            reloadConfig();

            if (!validateConfig()) {
                getLogger().severe("リロードされた設定ファイルに無効な値が含まれています。");
                return;
            }

            WorldBorderConfig.init(this);
            MilestoneConfig.init(getDataFolder().toPath());
            rewardService.reloadRewards();

            getLogger().info("設定ファイルがリロードされました。");
        } catch (Exception e) {
            getLogger().severe("設定リロード中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 設定値のバリデーション
     */
    private boolean validateConfig() {
        try {
            // ボーダー設定のチェック
            double initSize = getConfig().getDouble("border.initial_size", 100.0);
            double perChunk = getConfig().getDouble("border.expansion_per_chunk", 1.0);

            if (initSize < 0 || initSize > 60000000) {
                getLogger().severe("border.initial_size は 0 以上 60000000 以下である必要があります: " + initSize);
                return false;
            }

            if (perChunk < 0 || perChunk > 1000) {
                getLogger().severe("border.expansion_per_chunk は 0 以上 1000 以下である必要があります: " + perChunk);
                return false;
            }

            // DB設定のチェック
            String host = getConfig().getString("db.host");
            int port = getConfig().getInt("db.port", 3306);
            String dbName = getConfig().getString("db.name");
            String user = getConfig().getString("db.user");
            String pass = getConfig().getString("db.pass");

            if (host == null || host.trim().isEmpty()) {
                getLogger().severe("db.host が設定されていません。");
                return false;
            }

            if (port < 1 || port > 65535) {
                getLogger().severe("db.port は 1 以上 65535 以下である必要があります: " + port);
                return false;
            }

            if (dbName == null || dbName.trim().isEmpty()) {
                getLogger().severe("db.name が設定されていません。");
                return false;
            }

            if (user == null || user.trim().isEmpty()) {
                getLogger().severe("db.user が設定されていません。");
                return false;
            }

            return true;
        } catch (Exception e) {
            getLogger().severe("設定バリデーション中にエラーが発生しました: " + e.getMessage());
            return false;
        }
    }

    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public ChunkValidationCache getChunkCache() {
        return chunkCache;
    }
}