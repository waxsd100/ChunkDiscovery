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
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkDiscoveryPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // config.yml をコピー
        saveDefaultConfig();
        // JSON をコピー
        saveResource("milestones.json", false);
        saveResource("global_milestones.json", false);

        // ボーダー設定
        double initSize = getConfig().getDouble("border.initial_size");
        double perChunk = getConfig().getDouble("border.expansion_per_chunk");
        getServer().getWorlds().forEach(w -> w.getWorldBorder().setSize(initSize));

        // DB 初期化（テーブル生成も含む）
        DatabaseManager.init(
                getConfig().getString("db.host"),
                getConfig().getInt("db.port"),
                getConfig().getString("db.name"),
                getConfig().getString("db.user"),
                getConfig().getString("db.pass")
        );

        // マイルストーン JSON をロード
        MilestoneConfig.init(getDataFolder().toPath());

        // サービス初期化
        RewardService rewardService = new RewardService(this);
        DiscoveryService discoveryService = new DiscoveryService(
                new PlayerRepository(DatabaseManager.getDataSource()),
                new ChunkRepository(DatabaseManager.getDataSource()),
                rewardService,
                initSize,
                perChunk
        );

        // イベント/コマンド登録
        getServer().getPluginManager().registerEvents(
                new ChunkDiscoveryListener(discoveryService), this
        );
        getCommand("chunkdiscovery").setExecutor(
                new ChunkDiscoveryCommand(discoveryService)
        );
    }
}
