package io.wax100.chunkDiscovery.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

class ErrorHandlerTest {

    @Mock
    private Logger logger;
    
    @Mock
    private Player player;
    
    @Mock
    private CommandSender commandSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleAndNotify_WithPlayer() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        
        ErrorHandler.handleAndNotify(testException, logger, player, operation);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        verify(player).sendMessage(ChatColor.RED + "test operation中にエラーが発生しました。");
    }

    @Test
    void testHandleAndNotify_WithCommandSender() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        
        ErrorHandler.handleAndNotify(testException, logger, commandSender, operation);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        verify(commandSender).sendMessage(ChatColor.RED + "test operation中にエラーが発生しました。");
    }

    @Test
    void testHandleAndNotify_WithNullSender() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        
        ErrorHandler.handleAndNotify(testException, logger, (CommandSender) null, operation);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        // No message should be sent when sender is null
    }

    @Test
    void testLogError() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        
        ErrorHandler.logError(testException, logger, operation);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        // No message should be sent to any sender
    }

    @Test
    void testHandleWithCustomMessage() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        String customMessage = "Custom error message";
        
        ErrorHandler.handleWithCustomMessage(testException, logger, commandSender, operation, customMessage);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        verify(commandSender).sendMessage(ChatColor.RED + customMessage);
    }

    @Test
    void testHandleWithCustomMessage_NullSender() {
        Exception testException = new RuntimeException("Test exception");
        String operation = "test operation";
        String customMessage = "Custom error message";
        
        ErrorHandler.handleWithCustomMessage(testException, logger, null, operation, customMessage);
        
        verify(logger).severe("test operation中にエラーが発生しました: Test exception");
        // No message should be sent when sender is null
    }

    @Test
    void testHandleValidationError() {
        String message = "Validation failed";
        
        ErrorHandler.handleValidationError(commandSender, message);
        
        verify(commandSender).sendMessage(ChatColor.RED + message);
    }

    @Test
    void testHandleValidationError_NullSender() {
        String message = "Validation failed";
        
        ErrorHandler.handleValidationError(null, message);
        
        // No exception should be thrown and no message sent
    }

    @Test
    void testHandlePermissionError() {
        ErrorHandler.handlePermissionError(commandSender);
        
        verify(commandSender).sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
    }

    @Test
    void testHandlePermissionError_NullSender() {
        ErrorHandler.handlePermissionError(null);
        
        // No exception should be thrown and no message sent
    }
}