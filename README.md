# ChunkDiscovery Plugin

MinecraftのPaper/Spigotサーバー用プラグインで、プレイヤーのチャンク探索に応じてワールドボーダーを動的に拡張し、報酬を提供します。

## 特徴

- **動的ワールドボーダー拡張**: プレイヤーがチャンクを発見するとワールドボーダーが自動的に拡張
- **ワールド別設定**: オーバーワールド、ネザー、エンドそれぞれに異なる設定を適用
- **報酬システム**: チャンク発見時の報酬とマイルストーン報酬
- **MySQL統合**: データの永続化とサーバー間データ共有
- **非同期処理**: パフォーマンスを損なわない非同期データベース操作

## 必要環境

- **Minecraft**: 1.20.1以降
- **サーバー**: Paper/Spigot 1.20.1以降
- **Java**: 17以降
- **データベース**: MySQL 8.0以降

## インストール

1. [Releases](https://github.com/username/ChunkDiscovery/releases)から最新のjarファイルをダウンロード
2. サーバーの`plugins`フォルダに配置
3. サーバーを起動してプラグインを有効化
4. 生成された`config.yml`を編集
5. サーバーを再起動またはコマンドでリロード

## 設定

### データベース設定

```yaml
db:
  host: "localhost"
  port: 3306
  name: "minecraft_chunks"
  user: "minecraft_user"
  pass: "your_password"
```

### ワールドボーダー設定

```yaml
border:
  # デフォルト設定
  initial_size: 100.0
  expansion_per_chunk: 2.0
  
  # ワールドタイプ別設定
  world_types:
    overworld:
      initial_size: 16.0
      expansion_per_chunk: 4.0
    nether:
      initial_size: 32.0
      expansion_per_chunk: 16.0
    end:
      initial_size: 60000.0
      expansion_per_chunk: 2.0
```

### 報酬設定

```yaml
rewards:
  # 世界初発見報酬
  world_first:
    item:
      material: DIAMOND
      amount: 3
    experience: 100
  
  # 個人初発見報酬
  personal_first:
    item:
      material: EMERALD
      amount: 5
    experience: 10
  
  # マイルストーン報酬
  milestones:
    10:
      item:
        material: IRON_INGOT
        amount: 5
      experience: 50
    100:
      item:
        material: NETHERITE_INGOT
        amount: 1
      experience: 500
```

## コマンド

メインコマンド: `/chunkdiscovery` (エイリアス: `/cd`)

### サブコマンド

- `/cd stats` - 自分のチャンク発見統計を表示
- `/cd top [数]` - チャンク発見ランキングを表示（デフォルト10位まで）
- `/cd info` - 現在のワールドボーダー情報を表示
- `/cd check` - 現在地のチャンク発見状況を確認
- `/cd world` - 現在のワールドでの発見数を表示
- `/cd reload` - 設定ファイルをリロード（管理者のみ）

## 権限

- `chunkdiscovery.use` - 基本コマンドの使用（デフォルト: true）
- `chunkdiscovery.admin` - 管理者コマンドの使用（デフォルト: op）

## アーキテクチャ

### プロジェクト構造

```
src/main/java/io/wax100/chunkDiscovery/
├── ChunkDiscoveryPlugin.java          # メインプラグインクラス
├── initializer/
│   ├── PluginInitializer.java         # 初期化処理
│   └── ConfigValidator.java           # 設定値検証
├── service/
│   ├── DiscoveryService.java          # チャンク発見処理
│   └── RewardService.java             # 報酬システム
├── database/
│   ├── DatabaseManager.java           # データベース接続管理
│   ├── PlayerRepository.java          # プレイヤーデータ操作
│   ├── ChunkRepository.java           # チャンクデータ操作
│   └── WorldBorderRepository.java     # ワールドボーダーデータ操作
├── config/
│   └── WorldBorderConfig.java         # ワールドボーダー設定
├── manager/
│   ├── MilestoneConfig.java           # マイルストーン設定
│   ├── EffectManager.java             # エフェクト管理
│   └── RewardManager.java             # 報酬管理
├── model/
│   ├── PlayerData.java                # プレイヤーデータモデル
│   ├── DiscoveredChunk.java           # 発見チャンクモデル
│   └── RewardItem.java                # 報酬アイテムモデル
├── listener/
│   └── ChunkDiscoveryListener.java    # イベントリスナー
├── commands/
│   └── ChunkDiscoveryCommand.java     # コマンド処理
└── exception/
    ├── ChunkDiscoveryException.java   # 基底例外クラス
    ├── ConfigurationException.java    # 設定関連例外
    └── DatabaseException.java         # データベース関連例外
```

### データベーススキーマ

#### players テーブル
プレイヤーの基本情報と総発見数

```sql
CREATE TABLE players (
    player_id CHAR(36) NOT NULL PRIMARY KEY,
    total_chunks INT NOT NULL DEFAULT 0,
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### global_chunks テーブル
サーバー全体でのチャンク発見記録

```sql
CREATE TABLE global_chunks (
    world VARCHAR(64) NOT NULL,
    chunk_x INT NOT NULL,
    chunk_z INT NOT NULL,
    discovered_by CHAR(36) NOT NULL,
    discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (world, chunk_x, chunk_z)
);
```

#### player_chunks テーブル
プレイヤー個人のチャンク発見記録

```sql
CREATE TABLE player_chunks (
    player_id CHAR(36) NOT NULL,
    world VARCHAR(64) NOT NULL,
    chunk_x INT NOT NULL,
    chunk_z INT NOT NULL,
    discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, world, chunk_x, chunk_z)
);
```

#### world_borders テーブル
ワールドボーダーの現在状態

```sql
CREATE TABLE world_borders (
    world_name VARCHAR(64) NOT NULL PRIMARY KEY,
    border_size DOUBLE NOT NULL,
    total_chunks_discovered INT NOT NULL DEFAULT 0,
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 開発

### ビルド

```bash
./gradlew build
```

### テスト

```bash
./gradlew test
```

### 開発用サーバー起動

```bash
./gradlew runServer
```

### 依存関係

- **Paper API**: 1.20.1-R0.1-SNAPSHOT
- **HikariCP**: 5.0.1（コネクションプール）
- **MySQL Connector**: 8.0.33

## パフォーマンス

- **非同期処理**: データベース操作は全て非同期で実行
- **コネクションプール**: HikariCPによる効率的なDB接続管理
- **インデックス最適化**: 頻繁なクエリに対する適切なインデックス設定
- **メモリ効率**: 不要なデータのキャッシュを避けた軽量設計

## トラブルシューティング

### データベース接続エラー

1. MySQL サーバーが起動していることを確認
2. 接続情報（ホスト、ポート、ユーザー、パスワード）が正しいか確認
3. データベースとユーザーが存在するか確認
4. ファイアウォール設定を確認

### ワールドボーダーが更新されない

1. プレイヤーが新しいチャンクを発見しているか確認
2. データベース接続が正常か確認
3. コンソールでエラーログを確認

## ライセンス

MIT License

## 作者

wakokara

## 貢献

プルリクエストやイシューの報告を歓迎します。

## 更新履歴

### v1.0.0
- 初回リリース
- 基本的なチャンク発見とワールドボーダー拡張機能
- MySQL統合
- 報酬システム
- マイルストーン機能