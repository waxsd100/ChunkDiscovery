package io.wax100.chunkDiscovery.util;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * チャンクの岩盤チェック結果をキャッシュするユーティリティクラス
 */
public class ChunkValidationCache {
    private final ConcurrentHashMap<ChunkCoords, Boolean> validationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000; // メモリ使用量制限

    /**
     * 指定チャンクの最下層全域が岩盤かを判定（キャッシュ使用）
     * @param chunk 対象チャンク
     * @return 全ブロックが BEDROCK なら true
     */
    public boolean hasBedrockAtBottom(Chunk chunk) {
        ChunkCoords coords = new ChunkCoords(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );

        return validationCache.computeIfAbsent(coords, key -> {
            // キャッシュサイズ制限チェック
            if (validationCache.size() >= MAX_CACHE_SIZE) {
                // 古いエントリを削除（簡易LRU的な実装）
                validationCache.entrySet().removeIf(entry ->
                        validationCache.size() > MAX_CACHE_SIZE * 0.8
                );
            }

            return checkBedrockAtBottom(chunk);
        });
    }

    /**
     * 実際のチャンク検証を行う
     */
    private boolean checkBedrockAtBottom(Chunk chunk) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();  // 1.20.1 では -64
        int baseX = chunk.getX() << 4;   // chunkX * 16
        int baseZ = chunk.getZ() << 4;   // chunkZ * 16

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Block block = world.getBlockAt(baseX + dx, minY, baseZ + dz);
                if (block.getType() != Material.BEDROCK) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 特定のチャンクのキャッシュを無効化
     */
    public void invalidateChunk(Chunk chunk) {
        ChunkCoords coords = new ChunkCoords(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );
        validationCache.remove(coords);
    }

    /**
     * 特定のワールドのキャッシュをクリア
     */
    public void clearWorld(String worldName) {
        validationCache.entrySet().removeIf(entry ->
                entry.getKey().worldName().equals(worldName)
        );
    }

    /**
     * キャッシュ全体をクリア
     */
    public void clearCache() {
        validationCache.clear();
    }

    /**
     * キャッシュサイズを取得
     */
    public int getCacheSize() {
        return validationCache.size();
    }

    /**
     * キャッシュヒット率の統計情報を取得するためのメソッド
     */
    public void printCacheStats() {
        System.out.println("Chunk Validation Cache Size: " + validationCache.size());
    }

    /**
     * チャンク座標を表すレコードクラス
     */
    private record ChunkCoords(String worldName, int x, int z) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkCoords that = (ChunkCoords) obj;
            return x == that.x && z == that.z && Objects.equals(worldName, that.worldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, z);
        }
    }
}