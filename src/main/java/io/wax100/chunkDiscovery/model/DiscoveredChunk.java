package io.wax100.chunkDiscovery.model;

import java.time.LocalDateTime;

public record DiscoveredChunk(String world, int x, int z, String discoveredBy, LocalDateTime discoveredAt) {
}
