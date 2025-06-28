# ChunkDiscovery Plugin

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1+-brightgreen.svg)
![Spigot](https://img.shields.io/badge/Spigot-Paper%2FSpigot-yellow.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Tests](https://img.shields.io/badge/Tests-Passing-green.svg)
![Coverage](https://img.shields.io/badge/Coverage-36%25-yellow.svg)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-blue.svg)

MinecraftのPaper/Spigotサーバー用プラグインで、プレイヤーのチャンク探索に応じてワールドボーダーを動的に拡張し、包括的な報酬システムを提供します。

## ✨ 主要機能

- 🌍 **動的ワールドボーダー拡張**: チャンク発見に連動した自動ボーダー拡張
- 🎁 **二重報酬システム**: 個人発見報酬 + 世界初発見報酬
- 🏆 **マイルストーンシステム**: 個人・グローバル両方の達成報酬
- 🌐 **ワールド別設定**: オーバーワールド、ネザー、エンド個別設定
- 💾 **MySQL統合**: 永続化とサーバー間データ共有
- ⚡ **高パフォーマンス**: 非同期処理による軽量設計
- 🧪 **高品質**: 包括的なテストによる安定性保証
- 🔒 **セキュリティ**: OWASP依存関係チェック統合
- 📊 **品質管理**: Jacoco テストカバレッジ、ライセンス管理

## 📦 ダウンロード

### 最新版

[**🚀 最新リリースをダウンロード**](https://github.com/waxsd100/ChunkDiscovery/releases/latest)

### プレリリース版

CI/CDパイプラインにより、master ブランチへのコミット毎に自動的にプレリリース版が生成されます。

- ファイル名: `chunk-discovery-plugin-1.0-SNAPSHOT-all.jar`
- 自動テスト実行済み
- 最新の機能とバグ修正が含まれます

## 必要環境

- **Minecraft**: 1.20.1以降
- **サーバー**: Paper/Spigot 1.20.1以降
- **Java**: 17以降
- **データベース**: MySQL 8.0以降

## 🚀 インストール

1. [Releases](https://github.com/waxsd100/ChunkDiscovery/releases)から最新のjarファイルをダウンロード
2. サーバーの`plugins`フォルダに配置
3. サーバーを起動してプラグインを有効化
4. 生成された`config.yml`を編集
5. サーバーを再起動またはコマンドでリロード

## ⚙️ 設定

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

# スポーン地点設定
spawn:
  search_radius: 8
  nether:
    min_y: 64
    max_y: 96
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

## 🎮 コマンド

メインコマンド: `/chunkdiscovery` (エイリアス: `/cd`)

### サブコマンド一覧

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/cd stats [プレイヤー名]` | 自分または指定プレイヤーの統計表示 | `chunkdiscovery.use` |
| `/cd top [数]` | チャンク発見ランキング表示（デフォルト10位） | `chunkdiscovery.use` |
| `/cd info` | プラグイン情報とバージョン表示 | `chunkdiscovery.use` |
| `/cd check` | 現在地のチャンク発見状況を確認 | `chunkdiscovery.use` |
| `/cd world <ワールド名>` | 指定ワールドでの発見数とボーダー情報 | `chunkdiscovery.use` |
| `/cd reload` | 設定ファイルをリロード | `chunkdiscovery.reload` |

### タブ補完機能

- プレイヤー名の自動補完
- ワールド名の自動補完
- サブコマンドの自動補完

## 🔒 権限システム

| 権限 | 説明 | デフォルト |
|------|------|----------|
| `chunkdiscovery.use` | 基本コマンドの使用 | `true` |
| `chunkdiscovery.reload` | 設定リロード権限 | `op` |

## 🏗️ アーキテクチャ

### Clean Architecture 設計

プラグインは**Clean Architecture**原則に基づいて設計され、各レイヤーが明確に分離されています。

#### プロジェクト構造

```
src/main/java/io/wax100/chunkDiscovery/
├── 🎯 Core Layer (プラグインコア)
│   ├── ChunkDiscoveryPlugin.java          # メインプラグインクラス
│   └── Constants.java                     # 定数定義
│
├── 🔧 Initializer Layer (初期化処理)
│   ├── PluginInitializer.java             # プラグイン初期化
│   └── ConfigValidator.java               # 設定値検証
│
├── 🎮 Service Layer (ビジネスロジック)
│   ├── DiscoveryService.java              # チャンク発見処理
│   ├── RewardService.java                 # 報酬統制サービス
│   ├── RewardMessageHandler.java          # メッセージ処理
│   ├── MilestoneProcessor.java            # マイルストーン処理
│   └── DiscoveryType.java                 # 発見タイプ定義
│
├── 💾 Repository Layer (データアクセス)
│   ├── DatabaseManager.java               # データベース接続管理
│   ├── PlayerRepository.java              # プレイヤーデータ操作
│   ├── ChunkRepository.java               # チャンクデータ操作
│   └── WorldBorderRepository.java         # ワールドボーダーデータ操作
│
├── ⚙️ Configuration Layer (設定管理)
│   └── WorldBorderConfig.java             # ワールドボーダー設定
│
├── 🎛️ Manager Layer (機能管理)
│   ├── MilestoneConfig.java               # マイルストーン設定
│   ├── EffectManager.java                 # エフェクト管理
│   └── RewardManager.java                 # 報酬管理
│
├── 📊 Model Layer (データモデル)
│   ├── PlayerData.java                    # プレイヤーデータモデル
│   ├── DiscoveredChunk.java               # 発見チャンクモデル
│   └── RewardItem.java                    # 報酬アイテムモデル
│
├── 👂 Interface Layer (外部インターフェース)
│   ├── listener/
│   │   └── ChunkDiscoveryListener.java    # Bukkitイベントリスナー
│   └── commands/
│       └── ChunkDiscoveryCommand.java     # コマンド処理
│
├── 🛠️ Utility Layer (ユーティリティ)
│   ├── AsyncUtils.java                    # 非同期ユーティリティ
│   ├── ErrorHandler.java                  # エラーハンドリング
│   └── Validate.java                      # 入力値検証
│
└── ⚠️ Exception Layer (例外定義)
    ├── ChunkDiscoveryException.java       # 基底例外クラス
    ├── ConfigurationException.java        # 設定関連例外
    ├── DatabaseException.java             # データベース関連例外
    ├── ServiceException.java              # サービス関連例外
    └── ValidationException.java           # 検証関連例外
```

### 設計原則

- **単一責任原則**: 各クラスは明確に定義された単一の責任を持つ
- **依存性注入**: 疎結合な設計による高いテスタビリティ
- **非同期処理**: データベース操作の非同期化によるパフォーマンス最適化
- **エラーハンドリング**: 包括的な例外処理とグレースフル・デグラデーション

## 💾 データベーススキーマ

### players テーブル
プレイヤーの基本情報と総発見数

```sql
CREATE TABLE players (
    player_id CHAR(36) NOT NULL PRIMARY KEY,
    total_chunks INT NOT NULL DEFAULT 0,
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### global_chunks テーブル
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

### player_chunks テーブル
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

### world_borders テーブル
ワールドボーダーの現在状態

```sql
CREATE TABLE world_borders (
    world_name VARCHAR(64) NOT NULL PRIMARY KEY,
    border_size DOUBLE NOT NULL,
    total_chunks_discovered INT NOT NULL DEFAULT 0,
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 🔧 開発情報

### 🧪 品質管理

#### テストカバレッジ
- **現在のカバレッジ**: 36%
- **目標値**: 35%以上 ✅
- **テスト実行**: CI/CD パイプラインで自動実行

#### セキュリティチェック
- **OWASP Dependency Check**: NVD API キー設定時に実行
- **依存関係スキャン**: 既知の脆弱性チェック
- **ライセンス管理**: 法的コンプライアンス確保

#### ライセンス管理
- **許可ライセンス**: Apache 2.0, MIT, BSD 3-Clause, EPL 等
- **自動レポート**: HTML, JSON, CSV 形式で生成
- **コンプライアンス**: 全依存関係が許可ライセンス使用 ✅

### 🚀 CI/CD パイプライン

#### ワークフロー
1. **Test & Build**
   - テスト実行
   - カバレッジ生成
   - プラグインビルド
   - アーティファクト作成

2. **Release** (master ブランチのみ)
   - GitHub リリース自動作成
   - JARファイル配布
   - タイムスタンプ付きタグ

#### ビルドコマンド

```bash
# フルビルド
./gradlew build

# テスト実行
./gradlew test

# テストカバレッジレポート生成
./gradlew jacocoTestReport

# ライセンスレポート生成
./gradlew generateLicenseReport

# OWASP依存関係チェック (NVD_API_KEY必要)
export NVD_API_KEY=your_api_key_here
./gradlew dependencyCheckAnalyze

# Javadoc生成
./gradlew javadoc

# 開発用サーバー起動
./gradlew runServer

# ShadowJarビルド
./gradlew shadowJar
```

### 📦 依存関係

#### 実行時依存関係
- **Paper API**: 1.20.1-R0.1-SNAPSHOT
- **HikariCP**: 5.0.1（高性能コネクションプール）
- **MySQL Connector**: 8.0.33（データベースドライバ）

#### 開発・テスト依存関係
- **JUnit Jupiter**: 5.10.0（テストフレームワーク）
- **Mockito**: 5.6.0（モックフレームワーク）
- **Testcontainers**: 1.19.3（統合テスト用）
- **H2 Database**: 2.2.224（インメモリテスト用）

#### 品質管理ツール
- **Jacoco**: テストカバレッジ測定
- **OWASP Dependency Check**: セキュリティスキャン
- **License Report Plugin**: ライセンス管理

## ⚡ パフォーマンス

- **非同期処理**: データベース操作は全て非同期で実行
- **コネクションプール**: HikariCPによる効率的なDB接続管理
- **インデックス最適化**: 頻繁なクエリに対する適切なインデックス設定
- **メモリ効率**: 不要なデータのキャッシュを避けた軽量設計
- **スレッドセーフ**: 適切な並行処理制御

## 🔍 トラブルシューティング

### データベース接続エラー

1. MySQL サーバーが起動していることを確認
2. 接続情報（ホスト、ポート、ユーザー、パスワード）が正しいか確認
3. データベースとユーザーが存在するか確認
4. ファイアウォール設定を確認

### ワールドボーダーが更新されない

1. プレイヤーが新しいチャンクを発見しているか確認
2. データベース接続が正常か確認
3. コンソールでエラーログを確認

### スポーン地点の問題

1. `config.yml` でスポーン設定を確認
2. ワールドボーダーの中心が (0,0) に設定されているか確認
3. ネザーでの Y座標範囲設定を確認

## 📄 ライセンス

MIT License

## 👨‍💻 作者

wakokara

## 🤝 貢献

プルリクエストやイシューの報告を歓迎します。

### 開発環境セットアップ

1. Java 17+ をインストール
2. リポジトリをクローン: `git clone https://github.com/waxsd100/ChunkDiscovery.git`
3. 依存関係のダウンロード: `./gradlew build`
4. テスト実行: `./gradlew test`

## 📝 更新履歴

### v1.0-SNAPSHOT (最新)
- **CI/CDパイプライン統合**
  - GitHub Actions による自動テスト・ビルド・リリース
  - Jacoco テストカバレッジ (36%)
  - OWASP 依存関係セキュリティチェック
  - ライセンスコンプライアンス管理
  - 自動 Javadoc 生成
- **スポーン地点修正**
  - ワールドボーダー中心へのスポーン地点設定
  - オーバーワールド・ネザー両対応
  - 非同期処理による安全なスポーン検索
  - 設定可能な検索範囲とY座標制限
- **品質・保守性向上**
  - Clean Architecture の徹底
  - 包括的なエラーハンドリング
  - スレッドセーフティの確保
  - NPE対策の強化

### v1.1.0 (2024-12-25) 🎄
- **RewardServiceの大幅リファクタリング**
  - 単一責任原則に基づく設計改善（200行→148行、26%削減）
  - 4つのクラスに責任分離（RewardService、RewardMessageHandler、MilestoneProcessor、DiscoveryType）
  - 改善されたエラーハンドリングと適切なログレベル管理
- **包括的Spigotテストの追加**
  - 59個の新しいSpigot特化テスト追加
  - ChunkDiscoveryListenerTest（13テスト）
  - ChunkDiscoveryCommandTest（30テスト）
  - WorldBorderConfigSimpleTest（8テスト）
  - RewardMessageHandlerTest（8テスト）
- **アーキテクチャの改善**
  - 依存性注入による疎結合化
  - 遅延初期化によるパフォーマンス最適化
  - 型安全性の向上（enum使用）

### v1.0.0 (2024-12-20)
- **初回リリース**
- 基本的なチャンク発見とワールドボーダー拡張機能
- MySQL統合によるデータ永続化
- 二重報酬システム（個人発見・世界初発見）
- マイルストーン機能（個人・グローバル）
- 包括的なコマンドシステム
- 高品質テストカバレッジ（120+テスト）