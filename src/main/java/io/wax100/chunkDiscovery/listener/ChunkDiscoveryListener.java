package io.wax100.chunkDiscovery.listener;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
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

    // プレイヤーの最後のチャンク位置をキャッシュ
    private final Map<UUID, ChunkPosition> lastChunkPositions = new ConcurrentHashMap<>();

    public ChunkDiscoveryListener(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        try {
            Player player = e.getPlayer();
            Chunk toChunk = e.getTo().getChunk();

            // 同じチャンク内の移動は無視
            ChunkPosition currentPos = new ChunkPosition(
                    toChunk.getWorld().getName(),
                    toChunk.getX(),
                    toChunk.getZ()
            );

            ChunkPosition lastPos = lastChunkPositions.get(player.getUniqueId());
            if (currentPos.equals(lastPos)) {
                return;
            }

            // 位置を更新
            lastChunkPositions.put(player.getUniqueId(), currentPos);

            // 既に発見済みなら無視
            if (discoveryService.isDiscovered(player, toChunk)) {
                return;
            }

            // 岩盤じゃないので発見対象外(ENDは必ず失敗する)
            if (!isValidChunkChunk(player)) {
                return;
            }

            // 発見処理
            discoveryService.handleDiscovery(player, toChunk);

        } catch (Exception ex) {
            // エラーでもプレイヤーの移動を妨げない
            System.err.println("チャンク発見処理中にエラー: " + ex.getMessage());
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
        lastChunkPositions.remove(e.getPlayer().getUniqueId());
    }

    /**
     * チャンク位置
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

    /**
     * チャンクが有効な発見対象かどうかをチェック
     * - NORMAL: Y=-64の岩盤チェック
     * - NETHER: Y=0の岩盤チェック
     * - THE_END: 岩盤チェックはスキップ
     */
    private boolean isValidChunkChunk(Player player) {
        World world = player.getWorld();
        int x = player.getLocation().getBlockX();
        int minY = world.getMinHeight();
        int z = player.getLocation().getBlockZ();

        switch (world.getEnvironment()) {
            case NORMAL, NETHER -> {
                return world.getBlockAt(x, minY, z).getType() == Material.BEDROCK;
            }
            case THE_END -> {
                // エンドでは岩盤チェックは行わない
                return false;
            }
            default -> {
                // 他の環境は無視
                return true;
            }
        }
    }
}