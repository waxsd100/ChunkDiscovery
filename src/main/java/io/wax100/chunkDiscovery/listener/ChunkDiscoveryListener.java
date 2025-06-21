package io.wax100.chunkDiscovery.listener;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.util.ChunkValidator;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ChunkDiscoveryListener implements Listener {

    private final DiscoveryService discoveryService;

    public ChunkDiscoveryListener(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

        // すでに発見済みなら即時リターン
        if (discoveryService.isDiscovered(e.getPlayer(), e.getTo().getChunk())) return;

        // 同じチャンク内の移動は無視
        if (e.getFrom().getChunk().equals(e.getTo().getChunk())) return;

        Chunk chunk = e.getTo().getChunk();
        // 下層に完全に岩盤があるチャンクだけを“発見対象”とする
        if (!ChunkValidator.hasBedrockAtBottom(chunk)) return;

        // 発見処理
        discoveryService.handleDiscovery(e.getPlayer(), chunk);
    }
}