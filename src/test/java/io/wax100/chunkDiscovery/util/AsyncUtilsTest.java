package io.wax100.chunkDiscovery.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

class AsyncUtilsTest {

    private Logger mockLogger;
    private AtomicReference<String> lastLogMessage;

    @BeforeEach
    void setUp() {
        lastLogMessage = new AtomicReference<>();
        mockLogger = Logger.getAnonymousLogger();
        // Note: In a real test environment, you would mock the logger
        // For this test, we'll use a simple implementation
    }

    @Test
    void testExecuteAsync_Success() throws ExecutionException, InterruptedException {
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        
        CompletableFuture<String> future = AsyncUtils.executeAsync(
            () -> "success",
            throwable -> errorHandlerCalled.set(true)
        );
        
        String result = future.get();
        assertEquals("success", result);
        assertFalse(errorHandlerCalled.get());
    }

    @Test
    void testExecuteAsync_Exception() throws ExecutionException, InterruptedException {
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        AtomicReference<Throwable> capturedThrowable = new AtomicReference<>();
        
        CompletableFuture<String> future = AsyncUtils.executeAsync(
            () -> {
                throw new RuntimeException("Test exception");
            },
            throwable -> {
                errorHandlerCalled.set(true);
                capturedThrowable.set(throwable);
            }
        );
        
        String result = future.get();
        assertNull(result);
        assertTrue(errorHandlerCalled.get());
        assertNotNull(capturedThrowable.get());
        assertTrue(capturedThrowable.get().getMessage().contains("Test exception"));
    }

    @Test
    void testExecuteAsyncWithDefault_Success() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = AsyncUtils.executeAsyncWithDefault(
            () -> 42,
            mockLogger,
            "Test operation",
            -1
        );
        
        Integer result = future.get();
        assertEquals(42, result);
    }

    @Test
    void testExecuteAsyncWithDefault_Exception() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = AsyncUtils.executeAsyncWithDefault(
            () -> {
                throw new RuntimeException("Test exception");
            },
            mockLogger,
            "Test operation",
            -1
        );
        
        Integer result = future.get();
        assertEquals(-1, result);
    }

    @Test
    void testExecuteAsyncVoid_Success() throws ExecutionException, InterruptedException {
        AtomicBoolean operationCalled = new AtomicBoolean(false);
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncUtils.executeAsyncVoid(
            () -> operationCalled.set(true),
            throwable -> errorHandlerCalled.set(true)
        );
        
        future.get();
        assertTrue(operationCalled.get());
        assertFalse(errorHandlerCalled.get());
    }

    @Test
    void testExecuteAsyncVoid_Exception() throws ExecutionException, InterruptedException {
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        AtomicReference<Throwable> capturedThrowable = new AtomicReference<>();
        
        CompletableFuture<Void> future = AsyncUtils.executeAsyncVoid(
            () -> {
                throw new RuntimeException("Test exception");
            },
            throwable -> {
                errorHandlerCalled.set(true);
                capturedThrowable.set(throwable);
            }
        );
        
        future.get();
        assertTrue(errorHandlerCalled.get());
        assertNotNull(capturedThrowable.get());
        assertTrue(capturedThrowable.get().getMessage().contains("Test exception"));
    }

    @Test
    void testExecuteAsyncVoidWithLogging_Success() throws ExecutionException, InterruptedException {
        AtomicBoolean operationCalled = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncUtils.executeAsyncVoidWithLogging(
            () -> operationCalled.set(true),
            mockLogger,
            "Test operation"
        );
        
        future.get();
        assertTrue(operationCalled.get());
    }

    @Test
    void testExecuteAsyncVoidWithLogging_Exception() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = AsyncUtils.executeAsyncVoidWithLogging(
            () -> {
                throw new RuntimeException("Test exception");
            },
            mockLogger,
            "Test operation"
        );
        
        // Should not throw, just log the exception
        assertDoesNotThrow(() -> future.get());
    }
}