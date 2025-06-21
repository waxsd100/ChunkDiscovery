package io.wax100.chunkDiscovery.util;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * チャンク検証ユーティリティ
 * 1.20.1 では最下層が Y=-64 になるため、World#getMinHeight() を使用。
 */
public class ChunkValidator {
    /**
     * 指定チャンクの最下層全域が岩盤かを判定
     * @param chunk 対象チャンク
     * @return 全ブロックが BEDROCK なら true
     */
    public static boolean hasBedrockAtBottom(Chunk chunk) {
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
}
