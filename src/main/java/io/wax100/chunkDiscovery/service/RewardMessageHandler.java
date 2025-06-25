package io.wax100.chunkDiscovery.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * 報酬関連のメッセージ処理を担当するクラス
 */
public class RewardMessageHandler {
    
    /**
     * 世界初発見のメッセージを送信
     */
    public void sendWorldFirstMessage(Player discoverer, int totalChunks) {
        String message = formatWorldFirstMessage(discoverer.getName(), totalChunks);
        Bukkit.getServer().broadcastMessage(message);
    }
    
    /**
     * 個人初発見のメッセージを送信
     */
    public void sendPersonalFirstMessage(Player discoverer, int totalChunks) {
        String message = formatPersonalFirstMessage(totalChunks);
        discoverer.sendMessage(message);
    }
    
    /**
     * 個人マイルストーン達成メッセージを送信
     */
    public void sendPersonalMilestoneMessage(Player achiever, int milestoneCount, String customMessage, boolean broadcast) {
        if (broadcast) {
            String broadcastMsg = formatPersonalMilestoneBroadcast(achiever.getName(), milestoneCount);
            Bukkit.getServer().broadcastMessage(broadcastMsg);
        } else if (customMessage != null) {
            achiever.sendMessage(ChatColor.AQUA + customMessage);
        }
    }
    
    /**
     * グローバルマイルストーン達成メッセージを送信
     */
    public void sendGlobalMilestoneMessage(int milestoneCount, String customMessage) {
        String globalMsg = formatGlobalMilestoneMessage(milestoneCount);
        Bukkit.getServer().broadcastMessage(globalMsg);
        
        if (customMessage != null) {
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + customMessage);
        }
    }
    
    // Private formatting methods
    
    private String formatWorldFirstMessage(String playerName, int totalChunks) {
        return ChatColor.WHITE + "" + ChatColor.BOLD + playerName + 
               " さんが " + ChatColor.LIGHT_PURPLE + totalChunks + " 番目のチャンクを世界初発見！";
    }
    
    private String formatPersonalFirstMessage(int totalChunks) {
        return ChatColor.GREEN + "" + ChatColor.BOLD + totalChunks + " チャンクを発見して報酬を受け取りました！";
    }
    
    private String formatPersonalMilestoneBroadcast(String playerName, int milestoneCount) {
        return ChatColor.YELLOW + "" + ChatColor.BOLD + "[マイルストーン達成] " +
               ChatColor.YELLOW + playerName + " さんが " + milestoneCount + " チャンク発見を達成！";
    }
    
    private String formatGlobalMilestoneMessage(int milestoneCount) {
        return ChatColor.GOLD + "" + ChatColor.BOLD + "[グローバルマイルストーン達成] " +
               ChatColor.GOLD + "サーバー全体で " + milestoneCount + " チャンクが発見されました！";
    }
}