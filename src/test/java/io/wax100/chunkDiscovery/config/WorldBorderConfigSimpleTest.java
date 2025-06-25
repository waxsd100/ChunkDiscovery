package io.wax100.chunkDiscovery.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified tests for WorldBorderConfig that focus on the core functionality
 * without complex database mocking that causes issues in the test environment.
 */
class WorldBorderConfigSimpleTest {

    @Test
    void testWorldBorderSetting_ValidValues() {
        assertDoesNotThrow(() -> new WorldBorderConfig.WorldBorderSetting(100.0, 1.0));
        assertDoesNotThrow(() -> new WorldBorderConfig.WorldBorderSetting(0.0, 0.0));
        assertDoesNotThrow(() -> new WorldBorderConfig.WorldBorderSetting(60000000.0, 1000.0));
    }

    @Test
    void testWorldBorderSetting_InvalidInitialSize() {
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(-1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(60000001.0, 1.0));
    }

    @Test
    void testWorldBorderSetting_InvalidExpansionPerChunk() {
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1001.0));
    }

    @Test
    void testWorldBorderSetting_Getters() {
        WorldBorderConfig.WorldBorderSetting setting = 
            new WorldBorderConfig.WorldBorderSetting(200.0, 2.5);
        
        assertEquals(200.0, setting.initialSize());
        assertEquals(2.5, setting.expansionPerChunk());
    }

    @Test
    void testWorldBorderSetting_Equality() {
        WorldBorderConfig.WorldBorderSetting setting1 = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1.0);
        WorldBorderConfig.WorldBorderSetting setting2 = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1.0);
        WorldBorderConfig.WorldBorderSetting setting3 = 
            new WorldBorderConfig.WorldBorderSetting(200.0, 1.0);
        
        assertEquals(setting1, setting2);
        assertNotEquals(setting1, setting3);
        assertEquals(setting1.hashCode(), setting2.hashCode());
    }

    @Test
    void testWorldBorderSetting_ToString() {
        WorldBorderConfig.WorldBorderSetting setting = 
            new WorldBorderConfig.WorldBorderSetting(100.0, 1.0);
        
        String toString = setting.toString();
        assertTrue(toString.contains("100.0"));
        assertTrue(toString.contains("1.0"));
    }

    @Test
    void testWorldBorderSetting_BoundaryValues() {
        // Test boundary values
        assertDoesNotThrow(() -> new WorldBorderConfig.WorldBorderSetting(0.0, 0.0));
        assertDoesNotThrow(() -> new WorldBorderConfig.WorldBorderSetting(60000000.0, 1000.0));
        
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(-0.1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(60000000.1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(0.0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(0.0, 1000.1));
    }

    @Test
    void testWorldBorderSetting_ExceptionMessages() {
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(-1.0, 1.0));
        assertTrue(exception1.getMessage().contains("初期サイズは0以上60000000以下である必要があります"));
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> 
            new WorldBorderConfig.WorldBorderSetting(100.0, -1.0));
        assertTrue(exception2.getMessage().contains("チャンクあたりの拡張量は0以上1000以下である必要があります"));
    }
}