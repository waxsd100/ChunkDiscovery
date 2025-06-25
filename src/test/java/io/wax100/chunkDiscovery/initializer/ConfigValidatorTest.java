package io.wax100.chunkDiscovery.initializer;

import io.wax100.chunkDiscovery.Constants;
import org.bukkit.configuration.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigValidatorTest {

    @Mock
    private Configuration mockConfig;
    
    @Mock
    private Logger mockLogger;

    private ConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigValidator(mockConfig, mockLogger);
    }

    @Test
    void testValidate_AllValid() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(3306);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertTrue(result);
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testValidate_InvalidBorderSize_TooLarge() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0))
            .thenReturn(Constants.Validation.MAX_BORDER_SIZE + 1);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(1.0);

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe(contains("border.initial_size は 0 以上 60000000 以下である必要があります"));
    }

    @Test
    void testValidate_InvalidBorderSize_Negative() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(-1.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(1.0);

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe(contains("border.initial_size は 0 以上 60000000 以下である必要があります"));
    }

    @Test
    void testValidate_InvalidExpansionRate_TooLarge() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(100.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0))
            .thenReturn(Constants.Validation.MAX_EXPANSION_PER_CHUNK + 1);

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe(contains("border.expansion_per_chunk は 0 以上 1000 以下である必要があります"));
    }

    @Test
    void testValidate_InvalidHost_Null() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn(null);
        when(mockConfig.getInt("db.port", 3306)).thenReturn(3306);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe("db.host が設定されていません。");
    }

    @Test
    void testValidate_InvalidHost_Empty() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("   ");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(3306);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe("db.host が設定されていません。");
    }

    @Test
    void testValidate_InvalidPort_TooLarge() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(Constants.Validation.MAX_PORT + 1);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe(contains("db.port は 1 以上 65535 以下である必要があります"));
    }

    @Test
    void testValidate_InvalidPort_Zero() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(0);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe(contains("db.port は 1 以上 65535 以下である必要があります"));
    }

    @Test
    void testValidate_InvalidDatabaseName_Null() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(3306);
        when(mockConfig.getString("db.name")).thenReturn(null);
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe("db.name が設定されていません。");
    }

    @Test
    void testValidate_InvalidUsername_Empty() {
        // Arrange
        when(mockConfig.getDouble("border.initial_size", 100.0)).thenReturn(1000.0);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0)).thenReturn(2.5);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(3306);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("");

        // Act
        boolean result = validator.validate();

        // Assert
        assertFalse(result);
        verify(mockLogger).severe("db.user が設定されていません。");
    }

    @Test
    void testValidate_BoundaryValues() {
        // Arrange - Test exact boundary values
        when(mockConfig.getDouble("border.initial_size", 100.0))
            .thenReturn(Constants.Validation.MAX_BORDER_SIZE);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0))
            .thenReturn(Constants.Validation.MAX_EXPANSION_PER_CHUNK);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(Constants.Validation.MAX_PORT);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertTrue(result);
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testValidate_MinimumValues() {
        // Arrange - Test minimum boundary values
        when(mockConfig.getDouble("border.initial_size", 100.0))
            .thenReturn(Constants.Validation.MIN_BORDER_SIZE);
        when(mockConfig.getDouble("border.expansion_per_chunk", 1.0))
            .thenReturn(Constants.Validation.MIN_EXPANSION_PER_CHUNK);
        when(mockConfig.getString("db.host")).thenReturn("localhost");
        when(mockConfig.getInt("db.port", 3306)).thenReturn(Constants.Validation.MIN_PORT);
        when(mockConfig.getString("db.name")).thenReturn("minecraft_db");
        when(mockConfig.getString("db.user")).thenReturn("minecraft_user");

        // Act
        boolean result = validator.validate();

        // Assert
        assertTrue(result);
        verifyNoInteractions(mockLogger);
    }
}