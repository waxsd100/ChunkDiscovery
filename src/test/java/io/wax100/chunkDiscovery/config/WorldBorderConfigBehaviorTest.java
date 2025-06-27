package io.wax100.chunkDiscovery.config;

import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * WorldBorderConfigの基本動作をテストするクラス
 */
@ExtendWith(MockitoExtension.class)
class WorldBorderConfigBehaviorTest {

    @Mock
    private World overworldMock;
    
    @Mock
    private World netherMock;
    
    @Mock
    private World endMock;

    @Test
    void testWorldBorderSetting_CreationAndAccessors() {
        // 正常な値でのWorldBorderSetting作成
        WorldBorderConfig.WorldBorderSetting setting = 
            new WorldBorderConfig.WorldBorderSetting(50.0, 2.5);
        
        assertEquals(50.0, setting.initialSize());
        assertEquals(2.5, setting.expansionPerChunk());
    }

    @Test
    void testWorldBorderSetting_ValidationRules() {
        // 最小値でのテスト
        assertDoesNotThrow(() -> 
            new WorldBorderConfig.WorldBorderSetting(0.0, 0.0));
        
        // 最大値でのテスト
        assertDoesNotThrow(() -> 
            new WorldBorderConfig.WorldBorderSetting(60000000.0, 1000.0));
        
        // 無効な初期サイズ
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(-0.1, 2.0));
        assertTrue(ex1.getMessage().contains("初期サイズは0以上60000000以下"));
        
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(60000000.1, 2.0));
        assertTrue(ex2.getMessage().contains("初期サイズは0以上60000000以下"));
        
        // 無効な拡張量
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, -0.1));
        assertTrue(ex3.getMessage().contains("チャンクあたりの拡張量は0以上1000以下"));
        
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1000.1));
        assertTrue(ex4.getMessage().contains("チャンクあたりの拡張量は0以上1000以下"));
    }

    @Test
    void testWorldBorderSetting_EdgeCases() {
        // 境界値での動作確認
        WorldBorderConfig.WorldBorderSetting minSetting = 
            new WorldBorderConfig.WorldBorderSetting(0.0, 0.0);
        assertEquals(0.0, minSetting.initialSize());
        assertEquals(0.0, minSetting.expansionPerChunk());
        
        WorldBorderConfig.WorldBorderSetting maxSetting = 
            new WorldBorderConfig.WorldBorderSetting(60000000.0, 1000.0);
        assertEquals(60000000.0, maxSetting.initialSize());
        assertEquals(1000.0, maxSetting.expansionPerChunk());
        
        // 一般的な値
        WorldBorderConfig.WorldBorderSetting normalSetting = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 2.0);
        assertEquals(100.0, normalSetting.initialSize());
        assertEquals(2.0, normalSetting.expansionPerChunk());
    }

    @Test
    void testWorldBorderSetting_Equality() {
        WorldBorderConfig.WorldBorderSetting setting1 = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 2.0);
        WorldBorderConfig.WorldBorderSetting setting2 = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 2.0);
        WorldBorderConfig.WorldBorderSetting setting3 = 
            new WorldBorderConfig.WorldBorderSetting(50.0, 2.0);
        
        // 同じ値のオブジェクトは等しい
        assertEquals(setting1, setting2);
        assertEquals(setting1.hashCode(), setting2.hashCode());
        
        // 異なる値のオブジェクトは等しくない
        assertNotEquals(setting1, setting3);
        
        // toString()メソッドの動作確認
        String toString = setting1.toString();
        assertTrue(toString.contains("100.0"));
        assertTrue(toString.contains("2.0"));
    }

    @Test
    void testCalculateNewSize_MathematicalCorrectness() {
        // 設定値を使った計算のテスト
        WorldBorderConfig.WorldBorderSetting setting = 
            new WorldBorderConfig.WorldBorderSetting(16.0, 4.0);
        
        // 計算式: initialSize + (totalChunks * expansionPerChunk)
        
        // 0チャンクの場合
        double result0 = setting.initialSize() + (0 * setting.expansionPerChunk());
        assertEquals(16.0, result0);
        
        // 5チャンクの場合
        double result5 = setting.initialSize() + (5 * setting.expansionPerChunk());
        assertEquals(36.0, result5); // 16.0 + (5 * 4.0) = 36.0
        
        // 100チャンクの場合
        double result100 = setting.initialSize() + (100 * setting.expansionPerChunk());
        assertEquals(416.0, result100); // 16.0 + (100 * 4.0) = 416.0
    }

    @Test
    void testCalculateNewSize_LargeNumbers() {
        // 大きな数値での計算テスト
        WorldBorderConfig.WorldBorderSetting largeSetting = 
            new WorldBorderConfig.WorldBorderSetting(10000.0, 100.0);
        
        double result = largeSetting.initialSize() + (1000 * largeSetting.expansionPerChunk());
        assertEquals(110000.0, result); // 10000.0 + (1000 * 100.0) = 110000.0
    }

    @Test
    void testCalculateNewSize_SmallNumbers() {
        // 小さな数値での計算テスト
        WorldBorderConfig.WorldBorderSetting smallSetting = 
            new WorldBorderConfig.WorldBorderSetting(1.0, 0.1);
        
        double result = smallSetting.initialSize() + (10 * smallSetting.expansionPerChunk());
        assertEquals(2.0, result, 0.000001); // 1.0 + (10 * 0.1) = 2.0
    }

    @Test
    void testWorldBorderSetting_ImmutableBehavior() {
        // レコードクラスの不変性をテスト
        WorldBorderConfig.WorldBorderSetting setting = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 2.0);
        
        double originalInitialSize = setting.initialSize();
        double originalExpansionPerChunk = setting.expansionPerChunk();
        
        // 値は変更できない（レコードクラスなので）
        assertEquals(originalInitialSize, setting.initialSize());
        assertEquals(originalExpansionPerChunk, setting.expansionPerChunk());
        
        // 新しいオブジェクトを作成すれば異なる値を持てる
        WorldBorderConfig.WorldBorderSetting newSetting = 
            new WorldBorderConfig.WorldBorderSetting(200.0, 4.0);
        
        assertNotEquals(setting.initialSize(), newSetting.initialSize());
        assertNotEquals(setting.expansionPerChunk(), newSetting.expansionPerChunk());
    }
}