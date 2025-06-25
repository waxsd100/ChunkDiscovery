package io.wax100.chunkDiscovery.exception;

/**
 * ChunkDiscoveryプラグインで使用される基底例外クラス
 */
public class ChunkDiscoveryException extends Exception {
    
    public ChunkDiscoveryException(String message) {
        super(message);
    }
    
    public ChunkDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}