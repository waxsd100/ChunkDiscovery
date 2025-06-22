package io.wax100.chunkDiscovery.service;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.database.PlayerRepository;
import io.wax100.chunkDiscovery.database.ChunkRepository;
import io.wax100.chunkDiscovery.model.PlayerData;
import io.wax100.chunkDiscovery.config.WorldBorderConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * プレイヤーのチャンク発見処理を担当するサービスクラス
 */
public class DiscoveryService {
    private final PlayerRepository playerRepo;
    private final ChunkRepository chunkRepo;
    private final RewardService rewardService;
    private final ChunkDiscoveryPlugin plugin;

    public DiscoveryService(
            PlayerRepository playerRepo,
            ChunkRepository chunkRepo,
            RewardService rewardService,
            ChunkDiscoveryPlugin plugin
    ) {
        this.playerRepo = playerRepo;
        this.chunkRepo = chunkRepo;
        this.rewardService = rewardService;
        this.plugin = plugin;
    }

    /**
     * チャンク発見時の処理（ワールド別ボーダー対応）
     */
    public void handleDiscovery(Player player, Chunk chunk) {
        String pid = player.getUniqueId().toString();
        String worldName = chunk.getWorld().getName();

        // 非同期でDB操作を実行
        CompletableFuture.supplyAsync(() -> {
            try {
                // グローバル発見チェック
                boolean isGlobalFirst = chunkRepo.saveIfAbsent(chunk, pid);

                // 個人発見チェック
                boolean isPersonalFirst = playerRepo.saveIfAbsentChunk(pid, chunk);

                // 発見数を増加（全体）
                PlayerData globalData = null;
                if (isPersonalFirst) {
                    globalData = playerRepo.incrementTotalChunks(pid);
                }

                // ワールド別チャンク数を取得
                int worldChunks = playerRepo.getPlayerChunksInWorld(pid, worldName);

                return new DiscoveryResult(isGlobalFirst, isPersonalFirst, globalData, worldChunks);
            } catch (Exception e) {
                plugin.getLogger().severe("チャンク発見処理中にデータベースエラーが発生しました: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(result -> {
            // メインスレッドで結果を処理
            try {
                if (result.personalFirst && result.playerData != null) {
                    int totalGlobal = result.playerData.getTotalChunks();
                    int totalInWorld = result.worldChunks;

                    // ワールド別ボーダー拡張（そのワールドでの発見数を使用）
                    double newSize = WorldBorderConfig.calculateNewSize(player.getWorld(), totalInWorld);
                    WorldBorderConfig.updateBorderSize(player.getWorld(), newSize, totalInWorld);

                    plugin.getLogger().info(String.format(
                            "%s が %s でチャンクを発見: 全体 %d / %s内 %d / ボーダー %.1f",
                            player.getName(), worldName, totalGlobal, worldName, totalInWorld, newSize
                    ));

                    // 報酬付与（全体の発見数を使用）
                    rewardService.grantRewards(player, result.globalFirst, true, totalGlobal);

                    // グローバルマイルストーンチェック
                    if (result.globalFirst) {
                        rewardService.checkGlobalMilestones(getTotalDiscoveredChunks());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("チャンク発見結果処理中にエラーが発生しました: " + e.getMessage());
                player.sendMessage("§cチャンク発見の処理中にエラーが発生しました。");
            }
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable)).exceptionally(throwable -> {
            plugin.getLogger().severe("チャンク発見処理中に予期しないエラーが発生しました: " + throwable.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cチャンク発見に失敗しました。管理者にお知らせください。");
            });
            return null;
        });
    }

    /**
     * プレイヤーのチャンク発見総数を取得（非同期）
     */
    public CompletableFuture<Integer> getPlayerTotalChunksAsync(String playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerRepo.getTotalChunks(playerId);
            } catch (Exception e) {
                plugin.getLogger().severe("プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * プレイヤーのワールド別チャンク発見数を取得（非同期）
     */
    public CompletableFuture<Integer> getPlayerWorldChunksAsync(String playerId, String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerRepo.getPlayerChunksInWorld(playerId, worldName);
            } catch (Exception e) {
                plugin.getLogger().severe("ワールド別プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * プレイヤーのチャンク発見総数を取得（同期版 - コマンド用）
     */
    public int getPlayerTotalChunks(String playerId) {
        try {
            return playerRepo.getTotalChunks(playerId);
        } catch (Exception e) {
            plugin.getLogger().severe("プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
            return 0;
        }
    }

    /**
     * チャンク発見ランキングのトップNを取得（非同期）
     */
    public CompletableFuture<List<PlayerData>> getTopPlayersAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerRepo.getTopPlayers(limit);
            } catch (Exception e) {
                plugin.getLogger().severe("ランキング取得中にエラーが発生しました: " + e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * チャンク発見ランキングのトップNを取得（同期版 - コマンド用）
     */
    public List<PlayerData> getTopPlayers(int limit) {
        try {
            return playerRepo.getTopPlayers(limit);
        } catch (Exception e) {
            plugin.getLogger().severe("ランキング取得中にエラーが発生しました: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 指定したチャンクがプレイヤーにより既に発見されているか確認
     */
    public boolean isDiscovered(Player player, Chunk chunk) {
        String pid = player.getUniqueId().toString();
        try {
            return playerRepo.hasDiscoveredChunk(pid, chunk);
        } catch (Exception e) {
            plugin.getLogger().warning("チャンク発見状況確認中にエラーが発生しました: " + e.getMessage());
            return false; // エラー時は未発見として扱う
        }
    }

    /**
     * サーバー全体の発見済みチャンク総数を取得
     */
    private int getTotalDiscoveredChunks() {
        try {
            return chunkRepo.getTotalDiscoveredChunks();
        } catch (Exception e) {
            plugin.getLogger().severe("グローバル統計取得中にエラーが発生しました: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 特定ワールドでのプレイヤーのチャンク発見総数を取得
     * @param playerId プレイヤーID
     * @param worldName ワールド名
     * @return そのワールドでの発見数
     */
    public int getPlayerChunksInWorld(String playerId, String worldName) {
        try {
            return playerRepo.getPlayerChunksInWorld(playerId, worldName);
        } catch (Exception e) {
            plugin.getLogger().severe("ワールド別プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 発見結果を格納するためのレコードクラス
     */
    private record DiscoveryResult(boolean globalFirst, boolean personalFirst, PlayerData playerData, int worldChunks) {}
}