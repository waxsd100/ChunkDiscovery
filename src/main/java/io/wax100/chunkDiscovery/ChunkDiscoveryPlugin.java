package io.wax100.chunkDiscovery;

import io.wax100.chunkDiscovery.database.DatabaseManager;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.service.RewardService;
import io.wax100.chunkDiscovery.config.WorldBorderConfig;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.initializer.PluginInitializer;
import io.wax100.chunkDiscovery.exception.ConfigurationException;
import io.wax100.chunkDiscovery.exception.DatabaseException;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkDiscoveryPlugin extends JavaPlugin {

    private DiscoveryService discoveryService;
    private RewardService rewardService;

    @Override
    public void onEnable() {
        try {
            PluginInitializer initializer = new PluginInitializer(this);
            var result = initializer.initialize();
            
            this.discoveryService = result.discoveryService();
            this.rewardService = result.rewardService();
            
        } catch (ConfigurationException | DatabaseException e) {
            getLogger().severe("プラグインの初期化に失敗しました: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().severe("プラグインの初期化中に予期しないエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            DatabaseManager.shutdown();
            getLogger().info("ChunkDiscoveryPlugin が正常に無効化されました。");
        } catch (Exception e) {
            getLogger().severe("プラグインの無効化中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 設定ファイルをリロードする（ボーダーサイズ保持機能付き）
     */
    public void reloadPluginConfig() {
        try {
            getLogger().info("設定をリロードしています...");

            reloadConfig();
            
            var validator = new io.wax100.chunkDiscovery.initializer.ConfigValidator(getConfig(), getLogger());
            if (!validator.validate()) {
                getLogger().severe("リロードされた設定ファイルに無効な値が含まれています。");
                return;
            }

            // ワールドボーダー設定をリロード（DBから復元）
            WorldBorderConfig.reloadSettings();

            // マイルストーン設定をリロード
            MilestoneConfig.init(this);

            // 報酬設定をリロード
            rewardService.reloadRewards();

            getLogger().info("設定ファイルがリロードされました。ワールドボーダーサイズは保持されています。");
        } catch (Exception e) {
            getLogger().severe("設定リロード中にエラーが発生しました: " + e.getMessage());
        }
    }



    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }
}