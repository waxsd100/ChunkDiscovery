package io.wax100.chunkDiscovery;

/**
 * アプリケーション全体で使用される定数を管理するクラス
 */
public final class Constants {
    
    private Constants() {
        // ユーティリティクラスのためインスタンス化を防ぐ
    }

    /**
     * バリデーション関連の定数
     */
    public static final class Validation {
        public static final double MIN_BORDER_SIZE = 0.0;
        public static final double MAX_BORDER_SIZE = 60000000.0;
        public static final double MIN_EXPANSION_PER_CHUNK = 0.0;
        public static final double MAX_EXPANSION_PER_CHUNK = 1000.0;
        public static final int MIN_PORT = 1;
        public static final int MAX_PORT = 65535;
        public static final int MIN_TOP_LIMIT = 1;
        public static final int MAX_TOP_LIMIT = 50;
    }

    /**
     * データベース関連の定数
     */
    public static final class Database {
        public static final String PLAYERS_TABLE = "players";
        public static final String CHUNKS_TABLE = "global_chunks";
        public static final String WORLD_BORDERS_TABLE = "world_borders";
    }

    /**
     * ワールド関連の定数
     */
    public static final class World {
        public static final int CHUNK_SIZE = 16;
        public static final int BEDROCK_CHECK_POSITIONS = 4;
        public static final int MILESTONE_BROADCAST_THRESHOLD = 100;
    }

    /**
     * コマンド関連の定数
     */
    public static final class Commands {
        public static final String PERMISSION_USE = "chunkdiscovery.use";
        public static final String PERMISSION_ADMIN = "chunkdiscovery.admin";
        public static final String PERMISSION_RELOAD = "chunkdiscovery.reload";
    }

    /**
     * メッセージ関連の定数
     */
    public static final class Messages {
        public static final String NO_PERMISSION = "このコマンドを実行する権限がありません。";
        public static final String PLAYER_ONLY = "このコマンドはプレイヤーのみ実行できます。";
        public static final String RELOAD_SUCCESS = "設定ファイルがリロードされました。";
        public static final String INVALID_PLAYER = "指定されたプレイヤーが見つかりません。";
        public static final String DATABASE_ERROR = "データベースエラーが発生しました。";
    }
}