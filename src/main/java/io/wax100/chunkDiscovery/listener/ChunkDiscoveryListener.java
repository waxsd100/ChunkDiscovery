package io.wax100.chunkDiscovery.listener;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkDiscoveryListener implements Listener {

    private final DiscoveryService discoveryService;

    // プレイヤーの最後のチャンク位置をキャッシュして、同一チャンク内移動を効率的に除外
    private final Map<UUID, ChunkPosition> lastChunkPositions = new ConcurrentHashMap<>();

    public ChunkDiscoveryListener(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        try {
            Player player = e.getPlayer();
            Chunk toChunk = e.getTo().getChunk();

            // 同じチャンク内の移動かチェック（最適化）
            ChunkPosition currentPos = new ChunkPosition(
                    toChunk.getWorld().getName(),
                    toChunk.getX(),
                    toChunk.getZ()
            );

            ChunkPosition lastPos = lastChunkPositions.get(player.getUniqueId());
            if (currentPos.equals(lastPos)) {
                return; // 同じチャンク内移動なので処理しない
            }

            // 位置を更新
            lastChunkPositions.put(player.getUniqueId(), currentPos);

            // すでに発見済みなら即時リターン
            if (discoveryService.isDiscovered(player, toChunk)) {
                return;
            }

            // 下層に完全に岩盤があるチャンクだけを"発見対象"とする（キャッシュ使用）
            if (!discoveryService.isValidDiscoveryChunk(toChunk)) {
                return;
            }

            // 発見処理（非同期）
            discoveryService.handleDiscovery(player, toChunk);

        } catch (Exception ex) {
            // エラーが発生してもプレイヤーの移動を妨げないようにする
            // ログにエラーを記録
            if (discoveryService != null) {
                // DiscoveryServiceからプラグインインスタンスを取得してログ出力
                System.err.println("チャンク発見処理中にエラーが発生しました: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // プレイヤーがログインした際に初期位置を記録
        Chunk chunk = player.getLocation().getChunk();
        ChunkPosition pos = new ChunkPosition(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );
        lastChunkPositions.put(player.getUniqueId(), pos);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        // プレイヤーがログアウトした際にキャッシュをクリア（メモリリーク防止）
        lastChunkPositions.remove(e.getPlayer().getUniqueId());
    }

    /**
     * チャンク位置を表すレコードクラス
     */
    private record ChunkPosition(String worldName, int x, int z) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkPosition that = (ChunkPosition) obj;
            return x == that.x && z == that.z &&
                    java.util.Objects.equals(worldName, that.worldName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(worldName, x, z);
        }
    }
}