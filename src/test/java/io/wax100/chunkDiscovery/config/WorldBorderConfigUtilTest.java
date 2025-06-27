package io.wax100.chunkDiscovery.config;

import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldBorderConfigUtilTest {

    @Mock
    private World overworldMock;
    
    @Mock
    private World netherMock;
    
    @Mock
    private World endMock;
    
    @Mock
    private World customMock;

    @Test
    void testGetWorldTypeName_AllEnvironments() throws Exception {
        // ワールド環境設定
        when(overworldMock.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(netherMock.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(endMock.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(customMock.getEnvironment()).thenReturn(World.Environment.CUSTOM);
        
        // getWorldTypeNameメソッドをリフレクションで取得
        Method getWorldTypeName = WorldBorderConfig.class.getDeclaredMethod("getWorldTypeName", World.class);
        getWorldTypeName.setAccessible(true);
        
        // 各環境のテスト
        assertEquals("オーバーワールド", getWorldTypeName.invoke(null, overworldMock));
        assertEquals("Nether", getWorldTypeName.invoke(null, netherMock));
        assertEquals("End", getWorldTypeName.invoke(null, endMock));
        assertEquals("カスタム", getWorldTypeName.invoke(null, customMock));
    }

    @Test
    void testIsDangerousBlock_DangerousBlocks() throws Exception {
        Method isDangerousBlock = WorldBorderConfig.class.getDeclaredMethod("isDangerousBlock", Material.class);
        isDangerousBlock.setAccessible(true);
        
        // 危険なブロック
        assertTrue((Boolean) isDangerousBlock.invoke(null, Material.LAVA));
        assertTrue((Boolean) isDangerousBlock.invoke(null, Material.FIRE));
        assertTrue((Boolean) isDangerousBlock.invoke(null, Material.MAGMA_BLOCK));
        assertTrue((Boolean) isDangerousBlock.invoke(null, Material.CACTUS));
        
        // 安全なブロック
        assertFalse((Boolean) isDangerousBlock.invoke(null, Material.STONE));
        assertFalse((Boolean) isDangerousBlock.invoke(null, Material.GRASS_BLOCK));
        assertFalse((Boolean) isDangerousBlock.invoke(null, Material.DIRT));
    }

    @Test
    void testIsSafePlant_SafePlants() throws Exception {
        Method isSafePlant = WorldBorderConfig.class.getDeclaredMethod("isSafePlant", Material.class);
        isSafePlant.setAccessible(true);
        
        // 安全な植物（名前に"GRASS"を含む）
        assertTrue((Boolean) isSafePlant.invoke(null, Material.GRASS_BLOCK));
        assertTrue((Boolean) isSafePlant.invoke(null, Material.FERN));
        assertTrue((Boolean) isSafePlant.invoke(null, Material.OAK_SAPLING));
        
        // 非植物
        assertFalse((Boolean) isSafePlant.invoke(null, Material.STONE));
        assertFalse((Boolean) isSafePlant.invoke(null, Material.LAVA));
        assertFalse((Boolean) isSafePlant.invoke(null, Material.AIR));
    }

    @Test
    void testWorldBorderSetting_ValidValues() {
        // 有効な値でのテスト
        WorldBorderConfig.WorldBorderSetting setting = new WorldBorderConfig.WorldBorderSetting(100.0, 2.0);
        
        assertEquals(100.0, setting.initialSize());
        assertEquals(2.0, setting.expansionPerChunk());
    }

    @Test
    void testWorldBorderSetting_InvalidInitialSize() {
        // 初期サイズが負の値
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(-1.0, 2.0));
        
        // 初期サイズが大きすぎる
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(60000001.0, 2.0));
    }

    @Test
    void testWorldBorderSetting_InvalidExpansionPerChunk() {
        // 拡張量が負の値
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, -1.0));
        
        // 拡張量が大きすぎる
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1001.0));
    }

    @Test
    void testWorldBorderSetting_BoundaryValues() {
        // 境界値でのテスト
        assertDoesNotThrow(() -> 
            new WorldBorderConfig.WorldBorderSetting(0.0, 0.0));
        
        assertDoesNotThrow(() -> 
            new WorldBorderConfig.WorldBorderSetting(60000000.0, 1000.0));
    }
}