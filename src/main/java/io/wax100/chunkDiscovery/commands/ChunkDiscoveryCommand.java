package io.wax100.chunkDiscovery.commands;

import io.wax100.chunkDiscovery.ChunkDiscoveryPlugin;
import io.wax100.chunkDiscovery.service.DiscoveryService;
import io.wax100.chunkDiscovery.model.PlayerData;
import io.wax100.chunkDiscovery.config.WorldBorderConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;

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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /chunkdiscovery [stats|top|info|check|reload|world]");
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
                case "world":
                    handleWorldCommand(sender, args);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + sub);
                    sender.sendMessage(ChatColor.YELLOW + "Available commands: stats, top, info, check, reload, world");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("コマンド実行中にエラーが発生しました: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "コマンドの実行中にエラーが発生しました。");
        }

        return true;
    }

    private void handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
            return;
        }

        String targetPlayerId;
        String targetPlayerName;

        if (args.length > 1) {
            // 他のプレイヤーの統計を表示
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤー " + args[1] + " が見つかりません。");
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
            // 現在のワールドでの統計も取得
            String currentWorld = player.getWorld().getName();
            discoveryService.getPlayerWorldChunksAsync(targetPlayerId, currentWorld).thenAccept(worldTotal -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "=== " + targetPlayerName + " の統計 ===");
                    player.sendMessage(ChatColor.WHITE + "全体発見数: " + ChatColor.YELLOW + total);
                    player.sendMessage(ChatColor.WHITE + currentWorld + "での発見数: " + ChatColor.AQUA + worldTotal);

                    // 現在のワールドボーダーサイズも表示
                    double currentBorderSize = WorldBorderConfig.getCurrentBorderSize(currentWorld);
                    player.sendMessage(ChatColor.WHITE + currentWorld + "のボーダーサイズ: " + ChatColor.GOLD + String.format("%.1f", currentBorderSize));
                });
            });
        });
    }

    private void handleWorldCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /chunkdiscovery world <ワールド名>");
            player.sendMessage(ChatColor.GRAY + "利用可能なワールド: " +
                    Bukkit.getWorlds().stream().map(WorldInfo::getName).collect(Collectors.joining(", ")));
            return;
        }

        String worldName = args[1];
        if (Bukkit.getWorld(worldName) == null) {
            player.sendMessage(ChatColor.RED + "ワールド '" + worldName + "' が見つかりません。");
            return;
        }

        String playerId = player.getUniqueId().toString();

        // 指定されたワールドでの統計を表示
        discoveryService.getPlayerWorldChunksAsync(playerId, worldName).thenAccept(worldTotal -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.GREEN + "=== " + worldName + " での統計 ===");
                player.sendMessage(ChatColor.WHITE + "あなたの発見数: " + ChatColor.AQUA + worldTotal);

                // ワールドボーダー情報
                double currentBorderSize = WorldBorderConfig.getCurrentBorderSize(worldName);
                WorldBorderConfig.WorldBorderSetting setting = WorldBorderConfig.getSettingForWorld(worldName);

                player.sendMessage(ChatColor.WHITE + "現在のボーダーサイズ: " + ChatColor.GOLD + String.format("%.1f", currentBorderSize));
                player.sendMessage(ChatColor.WHITE + "初期サイズ: " + ChatColor.GRAY + setting.initialSize());
                player.sendMessage(ChatColor.WHITE + "チャンクあたりの拡張: " + ChatColor.GRAY + setting.expansionPerChunk());

                // 次のマイルストーンまでの距離を計算
                int nextMilestone = getNextMilestone(worldTotal);
                if (nextMilestone > 0) {
                    int remaining = nextMilestone - worldTotal;
                    player.sendMessage(ChatColor.WHITE + "次のマイルストーンまで: " + ChatColor.YELLOW + remaining + " チャンク");
                }
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
                sender.sendMessage(ChatColor.RED + "無効な数値です: " + args[1]);
                return;
            }
        }

        final int finalLimit = limit;

        // 非同期でランキングを取得
        discoveryService.getTopPlayersAsync(finalLimit).thenAccept(topPlayers -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "=== Top " + finalLimit + " チャンク発見者 ===");

                if (topPlayers.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "まだチャンクを発見したプレイヤーがいません。");
                    return;
                }

                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerData pd = topPlayers.get(i);
                    String rankMessage = ChatColor.YELLOW + "" + (i + 1) + ". " +
                            ChatColor.WHITE + getPlayerNameFromId(pd.getPlayerId()) + " - " +
                            ChatColor.AQUA + pd.getTotalChunks() + " チャンク";
                    sender.sendMessage(rankMessage);
                }
            });
        });
    }

    private void handleInfoCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== ChunkDiscovery Plugin 情報 ===");
        sender.sendMessage(ChatColor.WHITE + "バージョン: " + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.WHITE + "作者: " + plugin.getDescription().getAuthors().get(0));
    }

    private void handleCheckCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
            return;
        }

        // 現在のチャンクの有効性をチェック
        boolean isDiscovered = discoveryService.isDiscovered(player, player.getLocation().getChunk());
        String discoveredMessage = "このチャンクは" +
                (isDiscovered ? ChatColor.YELLOW + "既に発見済み" : ChatColor.GREEN + "未発見") +
                ChatColor.WHITE + "です。";

        player.sendMessage(discoveredMessage);

        // 現在のワールド情報
        String worldName = player.getWorld().getName();
        int worldChunks = discoveryService.getPlayerChunksInWorld(player.getUniqueId().toString(), worldName);
        player.sendMessage(ChatColor.WHITE + worldName + "での発見数: " + ChatColor.AQUA + worldChunks);
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("chunkdiscovery.reload")) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "設定をリロードしています...");

        try {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "設定のリロードが完了しました。ワールドボーダーサイズは保持されています。");
        } catch (Exception e) {
            plugin.getLogger().severe("設定リロード中にエラーが発生しました: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "設定のリロード中にエラーが発生しました。");
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

    /**
     * 次のマイルストーンを取得（簡易実装）
     */
    private int getNextMilestone(int current) {
        int[] milestones = {10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000};
        for (int milestone : milestones) {
            if (milestone > current) {
                return milestone;
            }
        }
        return -1; // マイルストーンなし
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("stats", "top", "info", "check", "world");
            if (sender.hasPermission("chunkdiscovery.reload")) {
                subcommands = Arrays.asList("stats", "top", "info", "check", "reload", "world");
            }
            return subcommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stats")) {
                // プレイヤー名の補完
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("world")) {
                // ワールド名の補完
                return Bukkit.getWorlds().stream()
                        .map(WorldInfo::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}