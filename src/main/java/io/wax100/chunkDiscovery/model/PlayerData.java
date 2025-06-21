package io.wax100.chunkDiscovery.model;

/**
 * プレイヤー別のチャンク発見統計を表すモデルクラス
 */
public class PlayerData {
    private final String playerId;
    private final int totalChunks;

    /**
     * コンストラクタ
     * @param playerId プレイヤーのUUID文字列
     * @param totalChunks 発見済みチャンク総数
     */
    public PlayerData(String playerId, int totalChunks) {
        this.playerId = playerId;
        this.totalChunks = totalChunks;
    }

    /**
     * プレイヤーID（UUID）を取得
     * @return UUID文字列
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * 発見済みチャンク総数を取得
     * @return チャンク数
     */
    public int getTotalChunks() {
        return totalChunks;
    }
}
