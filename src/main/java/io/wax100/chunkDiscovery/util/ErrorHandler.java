package io.wax100.chunkDiscovery.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * 統一されたエラーハンドリングユーティリティクラス
 */
public class ErrorHandler {
    
    /**
     * 例外をログに記録し、プレイヤーにエラーメッセージを送信
     * @param e 発生した例外
     * @param logger ログ出力用
     * @param player 通知対象プレイヤー（nullの場合は通知なし）
     * @param operation 実行していた操作名
     */
    public static void handleAndNotify(Exception e, Logger logger, Player player, String operation) {
        handleAndNotify(e, logger, (CommandSender) player, operation);
    }
    
    /**
     * 例外をログに記録し、コマンド送信者にエラーメッセージを送信
     * @param e 発生した例外
     * @param logger ログ出力用
     * @param sender 通知対象（nullの場合は通知なし）
     * @param operation 実行していた操作名
     */
    public static void handleAndNotify(Exception e, Logger logger, CommandSender sender, String operation) {
        logger.severe(operation + "中にエラーが発生しました: " + e.getMessage());
        e.printStackTrace();
        
        if (sender != null) {
            sender.sendMessage(ChatColor.RED + operation + "中にエラーが発生しました。");
        }
    }
    
    /**
     * 例外をログに記録のみ（通知なし）
     * @param e 発生した例外
     * @param logger ログ出力用
     * @param operation 実行していた操作名
     */
    public static void logError(Exception e, Logger logger, String operation) {
        logger.severe(operation + "中にエラーが発生しました: " + e.getMessage());
        e.printStackTrace();
    }
    
    /**
     * カスタムメッセージでエラーハンドリング
     * @param e 発生した例外
     * @param logger ログ出力用
     * @param sender 通知対象
     * @param operation 実行していた操作名
     * @param userMessage ユーザー向けメッセージ
     */
    public static void handleWithCustomMessage(Exception e, Logger logger, CommandSender sender, 
                                             String operation, String userMessage) {
        logger.severe(operation + "中にエラーが発生しました: " + e.getMessage());
        e.printStackTrace();
        
        if (sender != null) {
            sender.sendMessage(ChatColor.RED + userMessage);
        }
    }
    
    /**
     * 引数検証失敗時のハンドリング
     * @param sender コマンド送信者
     * @param message エラーメッセージ
     */
    public static void handleValidationError(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(ChatColor.RED + message);
        }
    }
    
    /**
     * 権限不足時のハンドリング
     * @param sender コマンド送信者
     */
    public static void handlePermissionError(CommandSender sender) {
        if (sender != null) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
        }
    }
}