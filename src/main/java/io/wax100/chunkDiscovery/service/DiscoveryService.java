package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.model.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * プレイヤーのチャンク発見処理を担当するサービスクラス
 */
public class DiscoveryService {
    private final PlayerRepository playerRepo;
    private final ChunkRepository chunkRepo;
    private final RewardService rewardService;
    private final double initialBorder;
    private final double perChunk;

    public DiscoveryService(
            PlayerRepository playerRepo,
            ChunkRepository chunkRepo,
            RewardService rewardService,
            double initialBorder,
            double perChunk
    ) {
        this.playerRepo = playerRepo;
        this.chunkRepo = chunkRepo;
        this.rewardService = rewardService;
        this.initialBorder = initialBorder;
        this.perChunk = perChunk;
    }

    /**
     * チャンク発見時の処理
     */
    public void handleDiscovery(Player player, Chunk chunk) {
        String pid = player.getUniqueId().toString();
        boolean isGlobalFirst = chunkRepo.saveIfAbsent(chunk, pid);
        boolean isPersonalFirst = playerRepo.saveIfAbsentChunk(pid, chunk);
        PlayerData data = playerRepo.incrementTotalChunks(pid);
        int total = data.getTotalChunks();

        double newSize = initialBorder + total * perChunk;
        player.getWorld().getWorldBorder().setSize(newSize);

        rewardService.grantRewards(player, isGlobalFirst, isPersonalFirst, total);
    }

    /**
     * プレイヤーのチャンク発見総数を取得
     */
    public int getPlayerTotalChunks(String playerId) {
        return playerRepo.getTotalChunks(playerId);
    }

    /**
     * チャンク発見ランキングのトップNを取得
     */
    public List<PlayerData> getTopPlayers(int limit) {
        return playerRepo.getTopPlayers(limit);
    }

    /**
     * 指定したチャンクがプレイヤーにより既に発見されているか確認
     */
    public boolean isDiscovered(Player player, Chunk chunk) {
        String pid = player.getUniqueId().toString();
        return playerRepo.hasDiscoveredChunk(pid, chunk);
    }
}
