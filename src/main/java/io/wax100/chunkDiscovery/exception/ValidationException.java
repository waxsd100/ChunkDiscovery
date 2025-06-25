package io.wax100.chunkDiscovery.exception;

/**
 * バリデーション失敗時の例外を表すクラス
 */
public class ValidationException extends ChunkDiscoveryException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}