package io.wax100.chunkDiscovery.initializer;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.DatabaseManager;
import io.wax100.chunkDiscovery.config.WorldBorderConfig;
import io.wax100.chunkDiscovery.manager.MilestoneConfig;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.service.RewardService;
import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.listener.ChunkDiscoveryListener;
import io.wax100.chunkDiscovery.commands.ChunkDiscoveryCommand;
import io.wax100.chunkDiscovery.exception.ConfigurationException;
import io.wax100.chunkDiscovery.exception.DatabaseException;

import java.util.Objects;

/**
 * プラグインの初期化処理を担当するクラス
 */
public class PluginInitializer {
    private final ChunkDiscoveryPlugin plugin;
    
    public PluginInitializer(ChunkDiscoveryPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * プラグインの初期化を実行
     */
    public InitializationResult initialize() throws ConfigurationException, DatabaseException {
        // 設定ファイルの保存と検証
        plugin.saveDefaultConfig();
        validateConfiguration();
        
        // データベース初期化
        initializeDatabase();
        
        // 各種設定の初期化
        initializeConfigurations();
        
        // サービス層の初期化
        ServiceContainer services = initializeServices();
        
        // イベントリスナーとコマンドの登録
        registerListenersAndCommands(services);
        
        plugin.getLogger().info("ChunkDiscoveryPlugin が正常に有効化されました。");
        return new InitializationResult(services.discoveryService(), services.rewardService());
    }
    
    private void validateConfiguration() throws ConfigurationException {
        ConfigValidator validator = new ConfigValidator(plugin.getConfig(), plugin.getLogger());
        if (!validator.validate()) {
            throw new ConfigurationException("設定ファイルに無効な値が含まれています。");
        }
    }
    
    private void initializeDatabase() throws DatabaseException {
        try {
            DatabaseManager.init(
                plugin.getConfig().getString("db.host"),
                plugin.getConfig().getInt("db.port"),
                plugin.getConfig().getString("db.name"),
                plugin.getConfig().getString("db.user"),
                plugin.getConfig().getString("db.pass")
            );
            plugin.getLogger().info("MySQL データベース接続が確立されました。");
        } catch (Exception e) {
            throw new DatabaseException("データベース初期化に失敗しました", e);
        }
    }
    
    private void initializeConfigurations() {
        try {
            WorldBorderConfig.init(plugin);
            
            // MilestoneConfigのインスタンスを作成
            MilestoneConfig milestoneConfig = new MilestoneConfig(plugin.getLogger());
            milestoneConfig.loadConfiguration(plugin.getConfig());
            MilestoneConfig.setInstance(milestoneConfig);
            
            plugin.getLogger().info("各種設定を読み込みました。");
        } catch (Exception e) {
            plugin.getLogger().warning("設定の読み込みに失敗しました: " + e.getMessage());
        }
    }
    
    private ServiceContainer initializeServices() {
        RewardService rewardService = new RewardService(plugin);
        DiscoveryService discoveryService = new DiscoveryService(
            new PlayerRepository(DatabaseManager.getDataSource()),
            new ChunkRepository(DatabaseManager.getDataSource()),
            rewardService,
            plugin
        );
        
        return new ServiceContainer(discoveryService, rewardService);
    }
    
    private void registerListenersAndCommands(ServiceContainer services) {
        plugin.getServer().getPluginManager().registerEvents(
            new ChunkDiscoveryListener(services.discoveryService()), 
            plugin
        );
        
        Objects.requireNonNull(plugin.getCommand("chunkdiscovery")).setExecutor(
            new ChunkDiscoveryCommand(services.discoveryService(), plugin)
        );
    }
    
    /**
     * 初期化結果を保持するレコード
     */
    public record InitializationResult(DiscoveryService discoveryService, RewardService rewardService) {}
    
    /**
     * サービスコンテナ
     */
    private record ServiceContainer(DiscoveryService discoveryService, RewardService rewardService) {}
}