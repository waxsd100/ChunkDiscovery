package io.wax100.chunkDiscovery.commands;

import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ChunkDiscoveryCommand implements CommandExecutor {
    private final DiscoveryService discoveryService;

    public ChunkDiscoveryCommand(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return true;
        }
        String pid = player.getUniqueId().toString();

        if (args.length == 0) {
            player.sendMessage("Usage: /chunkdiscovery [stats|top]");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "stats":
                int total = discoveryService.getPlayerTotalChunks(pid);
                player.sendMessage("あなたのチャンク発見数: " + total);
                break;
            case "top":
                List<PlayerData> top = discoveryService.getTopPlayers(10);
                player.sendMessage("=== Top 10 チャンク発見者 ===");
                for (int i = 0; i < top.size(); i++) {
                    PlayerData pd = top.get(i);
                    player.sendMessage((i+1) + ". " + pd.getPlayerId() + " - " + pd.getTotalChunks());
                }
                break;
            default:
                player.sendMessage("Unknown subcommand: " + sub);
        }
        return true;
    }
}
