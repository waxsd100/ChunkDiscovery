package io.wax100.chunkDiscovery.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 非同期処理のユーティリティクラス
 */
public class AsyncUtils {
    
    /**
     * 例外ハンドリング付きの非同期実行
     * @param operation 実行する処理
     * @param errorHandler エラーハンドラー
     * @param <T> 戻り値の型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsync(Supplier<T> operation, 
                                                        Consumer<Throwable> errorHandler) {
        return CompletableFuture.supplyAsync(operation)
                               .exceptionally(throwable -> {
                                   errorHandler.accept(throwable);
                                   return null;
                               });
    }
    
    /**
     * ログ付きの非同期実行（デフォルト値返却）
     * @param operation 実行する処理
     * @param logger ログ出力用
     * @param operationName 操作名
     * @param defaultValue エラー時のデフォルト値
     * @param <T> 戻り値の型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsyncWithDefault(Supplier<T> operation,
                                                                   Logger logger,
                                                                   String operationName,
                                                                   T defaultValue) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                logger.severe(operationName + "中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
                return defaultValue;
            }
        });
    }
    
    /**
     * void処理の非同期実行
     * @param operation 実行する処理
     * @param errorHandler エラーハンドラー
     * @return CompletableFuture<Void>
     */
    public static CompletableFuture<Void> executeAsyncVoid(Runnable operation,
                                                           Consumer<Throwable> errorHandler) {
        return CompletableFuture.runAsync(operation)
                               .exceptionally(throwable -> {
                                   errorHandler.accept(throwable);
                                   return null;
                               });
    }
    
    /**
     * ログ付きのvoid非同期実行
     * @param operation 実行する処理
     * @param logger ログ出力用
     * @param operationName 操作名
     * @return CompletableFuture<Void>
     */
    public static CompletableFuture<Void> executeAsyncVoidWithLogging(Runnable operation,
                                                                      Logger logger,
                                                                      String operationName) {
        return CompletableFuture.runAsync(() -> {
            try {
                operation.run();
            } catch (Exception e) {
                logger.severe(operationName + "中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}