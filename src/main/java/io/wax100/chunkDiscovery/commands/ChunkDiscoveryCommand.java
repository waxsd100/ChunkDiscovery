package io.wax100.chunkDiscovery.commands;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChunkDiscoveryCommand implements CommandExecutor, TabCompleter {
    private final DiscoveryService discoveryService;
    private final ChunkDiscoveryPlugin plugin;

    public ChunkDiscoveryCommand(DiscoveryService discoveryService, ChunkDiscoveryPlugin plugin) {
        this.discoveryService = discoveryService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /chunkdiscovery [stats|top|info|check|reload]").color(NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();

        try {
            switch (sub) {
                case "stats":
                    handleStatsCommand(sender, args);
                    break;
                case "top":
                    handleTopCommand(sender, args);
                    break;
                case "info":
                    handleInfoCommand(sender);
                    break;
                case "check":
                    handleCheckCommand(sender);
                    break;
                case "reload":
                    handleReloadCommand(sender);
                    break;
                default:
                    sender.sendMessage(Component.text("Unknown subcommand: " + sub).color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Available commands: stats, top, info, check, reload").color(NamedTextColor.YELLOW));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("コマンド実行中にエラーが発生しました: " + e.getMessage());
            sender.sendMessage(Component.text("コマンドの実行中にエラーが発生しました。").color(NamedTextColor.RED));
        }

        return true;
    }

    private void handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行可能です。").color(NamedTextColor.RED));
            return;
        }

        String targetPlayerId;
        String targetPlayerName;

        if (args.length > 1) {
            // 他のプレイヤーの統計を表示
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage(Component.text("プレイヤー " + args[1] + " が見つかりません。").color(NamedTextColor.RED));
                return;
            }
            targetPlayerId = targetPlayer.getUniqueId().toString();
            targetPlayerName = targetPlayer.getName();
        } else {
            // 自分の統計を表示
            targetPlayerId = player.getUniqueId().toString();
            targetPlayerName = player.getName();
        }

        // 非同期で統計を取得
        discoveryService.getPlayerTotalChunksAsync(targetPlayerId).thenAccept(total -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Component message = Component.text(targetPlayerName + " のチャンク発見数: " + total)
                        .color(NamedTextColor.GREEN);
                player.sendMessage(message);
            });
        });
    }

    private void handleTopCommand(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(limit, 50)); // 1-50の範囲に制限
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("無効な数値です: " + args[1]).color(NamedTextColor.RED));
                return;
            }
        }

        final int finalLimit = limit;

        // 非同期でランキングを取得
        discoveryService.getTopPlayersAsync(finalLimit).thenAccept(topPlayers -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(Component.text("=== Top " + finalLimit + " チャンク発見者 ===").color(NamedTextColor.GOLD));

                if (topPlayers.isEmpty()) {
                    sender.sendMessage(Component.text("まだチャンクを発見したプレイヤーがいません。").color(NamedTextColor.GRAY));
                    return;
                }

                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerData pd = topPlayers.get(i);
                    Component rankMessage = Component.text((i + 1) + ". ")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text(getPlayerNameFromId(pd.getPlayerId()) + " - " + pd.getTotalChunks())
                                    .color(NamedTextColor.WHITE));
                    sender.sendMessage(rankMessage);
                }
            });
        });
    }

    private void handleInfoCommand(CommandSender sender) {
        sender.sendMessage(Component.text("=== ChunkDiscovery Plugin 情報 ===").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("バージョン: " + plugin.getDescription().getVersion()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("作者: " + plugin.getDescription().getAuthors().get(0)).color(NamedTextColor.WHITE));

        // キャッシュ統計情報
        int cacheSize = plugin.getChunkCache().getCacheSize();
        sender.sendMessage(Component.text("チャンクキャッシュサイズ: " + cacheSize).color(NamedTextColor.WHITE));
    }

    private void handleCheckCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行可能です。").color(NamedTextColor.RED));
            return;
        }

        // 現在のチャンクの有効性をチェック
        boolean isValid = discoveryService.isValidDiscoveryChunk(player.getLocation().getChunk());
        boolean isDiscovered = discoveryService.isDiscovered(player, player.getLocation().getChunk());

        Component validMessage = Component.text("現在のチャンクは")
                .append(Component.text(isValid ? "有効" : "無効").color(isValid ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("な発見対象です。"));

        Component discoveredMessage = Component.text("このチャンクは")
                .append(Component.text(isDiscovered ? "既に発見済み" : "未発見").color(isDiscovered ? NamedTextColor.YELLOW : NamedTextColor.GREEN))
                .append(Component.text("です。"));

        player.sendMessage(validMessage);
        player.sendMessage(discoveredMessage);
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("chunkdiscovery.reload")) {
            sender.sendMessage(Component.text("このコマンドを実行する権限がありません。").color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("設定をリロードしています...").color(NamedTextColor.YELLOW));

        try {
            plugin.reloadPluginConfig();
            sender.sendMessage(Component.text("設定のリロードが完了しました。").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            plugin.getLogger().severe("設定リロード中にエラーが発生しました: " + e.getMessage());
            sender.sendMessage(Component.text("設定のリロード中にエラーが発生しました。").color(NamedTextColor.RED));
        }
    }

    private String getPlayerNameFromId(String playerId) {
        try {
            Player player = Bukkit.getPlayer(java.util.UUID.fromString(playerId));
            return player != null ? player.getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("stats", "top", "info", "check");
            if (sender.hasPermission("chunkdiscovery.reload")) {
                subcommands = Arrays.asList("stats", "top", "info", "check", "reload");
            }
            return subcommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            // プレイヤー名の補完
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}