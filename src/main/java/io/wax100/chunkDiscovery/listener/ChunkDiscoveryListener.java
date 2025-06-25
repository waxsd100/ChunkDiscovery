package io.wax100.chunkDiscovery.listener;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
            if (!shouldProcessMove(e)) {
                return;
            }
            
            Player player = e.getPlayer();
            Chunk toChunk = e.getTo().getChunk();
            
            updatePlayerPosition(player, toChunk);
            
            if (shouldDiscoverChunk(player, toChunk)) {
                discoveryService.handleDiscovery(player, toChunk);
            }
            
        } catch (Exception ex) {
            // エラーでもプレイヤーの移動を妨げない
            System.err.println("チャンク発見処理中にエラー: " + ex.getMessage());
        }
    }
    
    /**
     * 移動イベントを処理すべきかどうかを判定
     */
    private boolean shouldProcessMove(PlayerMoveEvent e) {
        if (e.getTo() == null) {
            return false;
        }
        
        // 同じチャンク内の移動は無視
        ChunkPosition currentPos = createChunkPosition(e.getTo().getChunk());
        ChunkPosition lastPos = lastChunkPositions.get(e.getPlayer().getUniqueId());
        
        return !currentPos.equals(lastPos);
    }
    
    /**
     * プレイヤーの位置を更新
     */
    private void updatePlayerPosition(Player player, Chunk chunk) {
        ChunkPosition currentPos = createChunkPosition(chunk);
        lastChunkPositions.put(player.getUniqueId(), currentPos);
    }
    
    /**
     * チャンクを発見すべきかどうかを判定
     */
    private boolean shouldDiscoverChunk(Player player, Chunk chunk) {
        // 既に発見済みなら無視
        if (discoveryService.isDiscovered(player, chunk)) {
            return false;
        }

        // 岩盤じゃないので発見対象外(ENDは必ず失敗する)
        if (!isValidChunkChunk(player)) {
            return false;
        }

        // チャンクの岩盤チェック
        return checkBedrockAtBottom(chunk);
    }
    
    /**
     * ChunkPositionオブジェクトを作成
     */
    private ChunkPosition createChunkPosition(Chunk chunk) {
        return new ChunkPosition(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        
        // プレイヤーがログインした際に初期位置を記録
        Chunk chunk = player.getLocation().getChunk();
        ChunkPosition pos = createChunkPosition(chunk);
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

    /**
     * 実際のチャンク検証を行う
     */
    private boolean checkBedrockAtBottom(Chunk chunk) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Block block = chunk.getBlock(dx, minY, dz);
                if (block.getType() != Material.BEDROCK) {
                    return false;
                }
            }
        }
        return true;
    }
}