# ChunkDiscovery Plugin - 機能仕様書

## 📋 目次

- [1. プラグイン概要](#1-プラグイン概要)
- [2. 機能仕様](#2-機能仕様)
- [3. 報酬システム](#3-報酬システム)
- [4. マイルストーンシステム](#4-マイルストーンシステム)
- [5. ワールドボーダーシステム](#5-ワールドボーダーシステム)
- [6. コマンドシステム](#6-コマンドシステム)
- [7. データベース仕様](#7-データベース仕様)
- [8. 設定仕様](#8-設定仕様)
- [9. イベント処理](#9-イベント処理)
- [10. エラーハンドリング](#10-エラーハンドリング)

---

## 1. プラグイン概要

### 1.1 目的
MinecraftのPaper/Spigotサーバーにおいて、プレイヤーのチャンク探索活動を促進し、探索に応じた報酬システムとワールドボーダーの動的拡張を提供する。

### 1.2 対象環境
- **Minecraft**: 1.20.1以降
- **サーバーソフトウェア**: Paper/Spigot 1.20.1以降
- **Java**: 17以降
- **データベース**: MySQL 8.0以降

### 1.3 主要機能
1. **チャンク発見システム**: プレイヤーの新規チャンク発見を自動検出
2. **二重報酬システム**: 個人発見報酬と世界初発見報酬
3. **マイルストーンシステム**: 個人・グローバル両方の達成報酬
4. **ワールドボーダー拡張**: 発見に連動した自動ボーダー拡張
5. **統計・ランキング**: プレイヤー統計とランキング表示

---

## 2. 機能仕様

### 2.1 チャンク発見システム

#### 2.1.1 発見条件
プレイヤーがチャンクを発見したと判定される条件：

1. **移動検出**: プレイヤーが新しいチャンクに移動した
2. **岩盤検証**: チャンクの最下層（Y座標最小値）がすべて岩盤ブロックである
3. **ワールド制限**: 以下のワールド環境で有効
   - **オーバーワールド** (NORMAL): Y=-64での岩盤チェック
   - **ネザー** (NETHER): Y=0での岩盤チェック
   - **エンド** (THE_END): 岩盤チェックなし（発見対象外）

#### 2.1.2 発見タイプ
チャンク発見は以下の3つのタイプに分類される：

| タイプ | 説明 | 報酬 |
|--------|------|------|
| **世界初発見** | サーバー全体で初めて発見されたチャンク | 世界初発見報酬 + エフェクト + 全体通知 |
| **個人初発見** | プレイヤーが初めて発見したチャンク | 個人発見報酬 + エフェクト + 個人通知 |
| **既発見** | プレイヤーが既に発見済みのチャンク | 報酬なし |

#### 2.1.3 検出ロジック
```
1. PlayerMoveEvent発生
2. 移動先チャンク取得
3. 前回位置との差分チェック
4. 岩盤検証実行
5. データベース照会（グローバル・個人）
6. 発見タイプ決定
7. 報酬・エフェクト・通知処理
8. データベース更新
9. マイルストーンチェック
10. ワールドボーダー更新
```

---

## 3. 報酬システム

### 3.1 報酬の種類

#### 3.1.1 世界初発見報酬
サーバー全体で初めて発見されたチャンクに対する報酬：

- **対象**: 発見者のみ
- **内容**: config.yml の `rewards.world_first` で設定
- **エフェクト**: お祝いエフェクト（花火・パーティクル）
- **通知**: サーバー全体への広播メッセージ
- **フォーマット**: "[プレイヤー名] さんが [X] 番目のチャンクを世界初発見！"

#### 3.1.2 個人初発見報酬
プレイヤーが個人として初めて発見したチャンクに対する報酬：

- **対象**: 発見者のみ
- **内容**: config.yml の `rewards.personal_first` で設定
- **エフェクト**: 花火エフェクト
- **通知**: 発見者への個人メッセージ
- **フォーマット**: "[X] チャンクを発見して報酬を受け取りました！"

#### 3.1.3 報酬アイテム仕様
各報酬は以下の要素で構成される：

```yaml
reward_example:
  items:                    # アイテムリスト
    - material: DIAMOND     # アイテム素材
      amount: 3             # 数量
      name: "特別なダイヤ"    # カスタム名（オプション）
      lore:                 # 説明文（オプション）
        - "チャンク発見の証"
        - "レア度: ★★★"
  experience: 100           # 経験値
  money: 1000              # 経済プラグイン連携（将来実装）
```

### 3.2 報酬配布処理

#### 3.2.1 配布フロー
1. **発見タイプ判定**: 世界初 or 個人初 or 既発見
2. **報酬計算**: 設定ファイルから対応報酬を取得
3. **インベントリチェック**: プレイヤーのインベントリ容量確認
4. **アイテム付与**: 直接インベントリに追加、満杯時は地面にドロップ
5. **経験値付与**: プレイヤーの経験値に加算
6. **エフェクト再生**: 指定されたエフェクト実行
7. **メッセージ送信**: 該当するメッセージ送信

#### 3.2.2 エラーハンドリング
- **インベントリ満杯**: アイテムを地面にドロップ + 警告メッセージ
- **不正なアイテム**: ログに警告を出力、処理続行
- **データベースエラー**: エラーログ出力、プレイヤーに通知

---

## 4. マイルストーンシステム

### 4.1 マイルストーンの種類

#### 4.1.1 個人マイルストーン
プレイヤー個人のチャンク発見数に基づく達成報酬：

- **対象**: 達成者のみ
- **判定**: プレイヤーの総発見チャンク数
- **報酬**: 達成者のみに付与
- **通知**: 個人メッセージ or 全体広播（設定により選択）
- **設定**: config.yml の `rewards.milestones.[数値]`

#### 4.1.2 グローバルマイルストーン
サーバー全体のチャンク発見数に基づく達成報酬：

- **対象**: 全オンラインプレイヤー
- **判定**: サーバー全体の発見チャンク数
- **報酬**: 全オンラインプレイヤーに付与
- **通知**: サーバー全体への広播メッセージ
- **重複防止**: 一度達成されたマイルストーンは再実行されない

### 4.2 マイルストーン設定仕様

#### 4.2.1 設定項目
```yaml
milestones:
  10:                       # マイルストーン値（発見チャンク数）
    items:                  # 報酬アイテム
      - material: IRON_INGOT
        amount: 5
    experience: 50          # 経験値報酬
    message: "10チャンク達成！" # カスタムメッセージ
    send_message: true      # メッセージ送信の有無
    broadcast: false        # 全体広播の有無（個人マイルストーンのみ）
    play_effects: true      # エフェクト再生の有無
```

#### 4.2.2 処理タイミング
- **個人マイルストーン**: チャンク発見時に即座にチェック
- **グローバルマイルストーン**: チャンク発見時に即座にチェック
- **チェック頻度**: 発見イベント毎（高頻度だが軽量処理）

### 4.3 マイルストーン管理

#### 4.3.1 履歴管理
- **個人マイルストーン**: 個別チェックのため履歴不要
- **グローバルマイルストーン**: メモリ内で達成済みマイルストーンを管理
- **重複防止**: ConcurrentHashSet による thread-safe な管理

#### 4.3.2 リセット機能
- **設定リロード時**: グローバルマイルストーン履歴をリセット
- **テスト用メソッド**: 手動でのリセット機能提供
- **永続化**: 現在はメモリ管理（将来的にDBでの永続化も検討）

---

## 5. ワールドボーダーシステム

### 5.1 ワールドボーダー設定

#### 5.1.1 設定階層
ワールドボーダーの設定は以下の優先順位で適用される：

1. **個別ワールド設定** (`border.worlds.[ワールド名]`)
2. **ワールドタイプ設定** (`border.world_types.[タイプ名]`)
3. **デフォルト設定** (`border.initial_size`, `border.expansion_per_chunk`)

#### 5.1.2 ワールドタイプ対応
| ワールドタイプ | 設定キー | 説明 |
|---------------|----------|------|
| オーバーワールド | `overworld`, `normal` | 通常の地上世界 |
| ネザー | `nether` | ネザー世界 |
| エンド | `end`, `the_end` | エンド世界 |

#### 5.1.3 ボーダーサイズ計算
```
新しいボーダーサイズ = 初期サイズ + (発見チャンク数 × チャンクあたり拡張量)
```

### 5.2 ボーダー更新処理

#### 5.2.1 更新タイミング
- **世界初発見時**: 該当ワールドのボーダーを即座に拡張
- **サーバー起動時**: データベースから保存されたサイズを復元
- **設定リロード時**: 現在のサイズを保持したまま設定更新

#### 5.2.2 データ永続化
- **保存先**: MySQL の `world_borders` テーブル
- **保存内容**: ワールド名、現在のボーダーサイズ、発見チャンク数
- **更新頻度**: チャンク発見毎に即座に保存

### 5.3 ボーダー制限値

#### 5.3.1 設定値制限
- **初期サイズ**: 0.0 ～ 60,000,000.0
- **拡張量**: 0.0 ～ 1,000.0
- **範囲外値**: 設定時に IllegalArgumentException をスロー

#### 5.3.2 Minecraftの制限
- **最大サイズ**: Minecraft自体の制限（通常60,000,000ブロック）
- **最小サイズ**: 技術的には1.0ブロックまで可能

---

## 6. コマンドシステム

### 6.1 コマンド一覧

#### 6.1.1 メインコマンド
- **コマンド**: `/chunkdiscovery`, `/cd`
- **権限**: `chunkdiscovery.use`
- **説明**: ChunkDiscoveryプラグインのメインコマンド

#### 6.1.2 サブコマンド詳細

##### `/cd stats [プレイヤー名]`
- **機能**: プレイヤーのチャンク発見統計を表示
- **権限**: `chunkdiscovery.use`
- **引数**: 
  - なし: 実行者の統計を表示
  - プレイヤー名: 指定プレイヤーの統計を表示
- **表示内容**:
  - 全体発見数
  - 現在ワールドでの発見数
  - 現在ワールドのボーダーサイズ
- **非同期処理**: データベースアクセスは非同期で実行

##### `/cd top [数]`
- **機能**: チャンク発見ランキングを表示
- **権限**: `chunkdiscovery.use`
- **引数**:
  - なし: 上位10位まで表示
  - 数値: 指定された数（1-50の範囲）まで表示
- **表示形式**: `順位. プレイヤー名 - X チャンク`
- **データソース**: 全プレイヤーの総発見数

##### `/cd info`
- **機能**: プラグイン情報を表示
- **権限**: `chunkdiscovery.use`
- **表示内容**:
  - プラグインバージョン
  - 作者情報
  - 基本的な使用方法

##### `/cd check`
- **機能**: 現在地のチャンク情報を表示
- **権限**: `chunkdiscovery.use`
- **表示内容**:
  - 現在チャンクの発見状況（発見済み/未発見）
  - 現在ワールドでの発見数
- **実行制限**: プレイヤーのみ実行可能

##### `/cd world <ワールド名>`
- **機能**: 指定ワールドの詳細情報を表示
- **権限**: `chunkdiscovery.use`
- **引数**: ワールド名（タブ補完対応）
- **表示内容**:
  - プレイヤーの該当ワールドでの発見数
  - 現在のボーダーサイズ
  - 初期サイズとチャンクあたり拡張量
  - 次のマイルストーンまでの距離

##### `/cd reload`
- **機能**: プラグイン設定をリロード
- **権限**: `chunkdiscovery.reload`
- **処理内容**:
  - config.yml の再読み込み
  - 設定値の検証
  - ワールドボーダー設定の更新（サイズ保持）
  - マイルストーン設定の更新
  - 報酬設定の更新

### 6.2 タブ補完機能

#### 6.2.1 補完対象
- **サブコマンド**: 権限に応じた利用可能サブコマンド
- **プレイヤー名**: オンラインプレイヤーのリアルタイム補完
- **ワールド名**: サーバー内全ワールドの補完

#### 6.2.2 権限連動
- **reload**: `chunkdiscovery.reload` 権限保持者のみ表示
- **基本コマンド**: `chunkdiscovery.use` 権限保持者に表示

---

## 7. データベース仕様

### 7.1 テーブル設計

#### 7.1.1 players テーブル
プレイヤーの基本情報と発見統計を管理

```sql
CREATE TABLE players (
    player_id CHAR(36) NOT NULL PRIMARY KEY COMMENT 'プレイヤーUUID',
    total_chunks INT NOT NULL DEFAULT 0 COMMENT '総発見チャンク数',
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最終更新日時',
    INDEX idx_total_chunks (total_chunks DESC) COMMENT 'ランキング用インデックス'
);
```

#### 7.1.2 global_chunks テーブル
サーバー全体でのチャンク発見記録を管理

```sql
CREATE TABLE global_chunks (
    world VARCHAR(64) NOT NULL COMMENT 'ワールド名',
    chunk_x INT NOT NULL COMMENT 'チャンクX座標',
    chunk_z INT NOT NULL COMMENT 'チャンクZ座標',
    discovered_by CHAR(36) NOT NULL COMMENT '発見者UUID',
    discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '発見日時',
    PRIMARY KEY (world, chunk_x, chunk_z),
    INDEX idx_discovered_by (discovered_by) COMMENT '発見者検索用',
    INDEX idx_discovered_at (discovered_at DESC) COMMENT '発見日時検索用'
);
```

#### 7.1.3 player_chunks テーブル
プレイヤー個人のチャンク発見記録を管理

```sql
CREATE TABLE player_chunks (
    player_id CHAR(36) NOT NULL COMMENT 'プレイヤーUUID',
    world VARCHAR(64) NOT NULL COMMENT 'ワールド名',
    chunk_x INT NOT NULL COMMENT 'チャンクX座標',
    chunk_z INT NOT NULL COMMENT 'チャンクZ座標',
    discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '発見日時',
    PRIMARY KEY (player_id, world, chunk_x, chunk_z),
    INDEX idx_player_world (player_id, world) COMMENT 'プレイヤー・ワールド別検索用',
    INDEX idx_discovered_at (discovered_at DESC) COMMENT '発見日時検索用'
);
```

#### 7.1.4 world_borders テーブル
ワールドボーダーの現在状態を管理

```sql
CREATE TABLE world_borders (
    world_name VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'ワールド名',
    border_size DOUBLE NOT NULL COMMENT '現在のボーダーサイズ',
    total_chunks_discovered INT NOT NULL DEFAULT 0 COMMENT '該当ワールドの総発見チャンク数',
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最終更新日時'
);
```

### 7.2 データアクセス仕様

#### 7.2.1 Repository パターン
各テーブルに対応するRepositoryクラスでデータアクセスを抽象化：

- **PlayerRepository**: プレイヤー関連データの操作
- **ChunkRepository**: チャンク発見記録の操作
- **WorldBorderRepository**: ワールドボーダー情報の操作

#### 7.2.2 トランザクション管理
- **チャンク発見処理**: 複数テーブルの更新を単一トランザクションで実行
- **エラー時のロールバック**: データ整合性を保証
- **デッドロック対策**: 適切なロック順序の定義

#### 7.2.3 コネクションプール
- **HikariCP**: 高性能コネクションプール使用
- **最大接続数**: 設定可能（デフォルト: 10）
- **接続タイムアウト**: 30秒
- **アイドルタイムアウト**: 600秒

---

## 8. 設定仕様

### 8.1 設定ファイル構造

#### 8.1.1 config.yml 全体構造
```yaml
# データベース設定
db:
  host: "localhost"
  port: 3306
  name: "minecraft_chunks"
  user: "minecraft_user"
  pass: "password"
  
# ワールドボーダー設定
border:
  # デフォルト設定
  initial_size: 100.0
  expansion_per_chunk: 1.0
  
  # ワールド別個別設定
  worlds:
    world:
      initial_size: 16.0
      expansion_per_chunk: 4.0
    world_nether:
      initial_size: 32.0
      expansion_per_chunk: 16.0
  
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

# 報酬設定
rewards:
  # 世界初発見報酬
  world_first:
    items:
      - material: DIAMOND
        amount: 3
        name: "世界初発見の証"
        lore:
          - "この栄誉ある発見を称える"
          - "レア度: ★★★★★"
    experience: 100
    
  # 個人初発見報酬
  personal_first:
    items:
      - material: EMERALD
        amount: 5
    experience: 10
    
  # マイルストーン報酬
  milestones:
    10:
      items:
        - material: IRON_INGOT
          amount: 5
      experience: 50
      message: "10チャンク発見を達成しました！"
      send_message: true
      broadcast: false
      play_effects: true
    25:
      items:
        - material: GOLD_INGOT
          amount: 3
      experience: 100
      message: "25チャンク発見を達成！"
      send_message: true
      broadcast: true
      play_effects: true
    100:
      items:
        - material: NETHERITE_INGOT
          amount: 1
          name: "探索者の証"
          lore:
            - "100チャンク発見の証明"
            - "真の探索者にのみ与えられる"
      experience: 500
      message: "100チャンク発見の偉業を達成！"
      send_message: true
      broadcast: true
      play_effects: true
```

### 8.2 設定値検証

#### 8.2.1 必須設定項目
- **db.host, db.port, db.name, db.user, db.pass**: データベース接続情報
- **border.initial_size, border.expansion_per_chunk**: デフォルトボーダー設定

#### 8.2.2 値の範囲制限
- **border.initial_size**: 0.0 ～ 60,000,000.0
- **border.expansion_per_chunk**: 0.0 ～ 1,000.0
- **rewards.*.experience**: 0 ～ Integer.MAX_VALUE
- **rewards.*.items.amount**: 1 ～ 64

#### 8.2.3 設定エラー処理
- **必須項目欠如**: ConfigurationException をスロー、プラグイン初期化停止
- **範囲外値**: 警告ログ出力、デフォルト値で代替
- **不正なマテリアル**: 警告ログ出力、該当アイテムをスキップ

---

## 9. イベント処理

### 9.1 プレイヤー移動イベント

#### 9.1.1 PlayerMoveEvent 処理フロー
```
1. イベント受信 (EventPriority.MONITOR, ignoreCancelled = true)
2. 移動先座標の null チェック
3. チャンク位置変更の検出
4. 位置キャッシュの更新
5. 岩盤検証の実行
6. データベース照会（非同期）
7. 発見タイプの判定
8. 報酬・エフェクト処理（メインスレッド）
9. データベース更新（非同期）
10. マイルストーンチェック
11. ワールドボーダー更新
```

#### 9.1.2 最適化処理
- **同一チャンク内移動**: 無視（パフォーマンス向上）
- **位置キャッシュ**: ConcurrentHashMap による高速アクセス
- **非同期処理**: データベースアクセスはすべて非同期実行

### 9.2 プレイヤー参加・退出イベント

#### 9.2.1 PlayerJoinEvent
- **処理内容**: プレイヤーの初期位置をキャッシュに記録
- **目的**: 移動検出の基準点設定

#### 9.2.2 PlayerQuitEvent
- **処理内容**: プレイヤーの位置キャッシュを削除
- **目的**: メモリリーク防止

---

## 10. エラーハンドリング

### 10.1 例外階層

#### 10.1.1 基底例外クラス
```java
public class ChunkDiscoveryException extends Exception {
    // 基底例外クラス
}
```

#### 10.1.2 特化例外クラス
- **ConfigurationException**: 設定関連エラー
- **DatabaseException**: データベース関連エラー
- **ServiceException**: サービス処理エラー
- **ValidationException**: 入力値検証エラー

### 10.2 エラー処理方針

#### 10.2.1 グレースフル・デグラデーション
- **データベースエラー**: ログ出力後、処理続行（機能制限状態）
- **設定エラー**: デフォルト値で代替、警告出力
- **プレイヤー操作エラー**: エラーメッセージ表示、安全な状態に復帰

#### 10.2.2 ログレベル管理
- **SEVERE**: 致命的エラー（プラグイン停止レベル）
- **WARNING**: 警告（機能制限の可能性）
- **INFO**: 通常の情報（起動・停止・設定変更）
- **FINE**: デバッグ情報（開発・トラブルシューティング用）

#### 10.2.3 プレイヤー通知
- **エラー発生時**: 分かりやすいメッセージでプレイヤーに通知
- **詳細情報**: サーバーログに記録、プレイヤーには簡潔に説明
- **復旧方法**: 可能な場合は復旧手順を案内

---

## 🔄 この仕様書の管理

- **最終更新**: 2024-12-25
- **バージョン**: v1.1.0
- **更新者**: Claude Code Assistant
- **次回更新予定**: 機能追加・仕様変更時

この仕様書は、ChunkDiscoveryプラグインの全機能について詳細に記述したものです。開発・運用・トラブルシューティングの際の参照資料として活用してください。