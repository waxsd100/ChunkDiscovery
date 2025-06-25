package io.wax100.chunkDiscovery.exception;

/**
 * データベース操作で発生する例外
 */
public class DatabaseException extends ChunkDiscoveryException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}