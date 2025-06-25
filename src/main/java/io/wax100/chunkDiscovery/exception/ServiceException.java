package io.wax100.chunkDiscovery.exception;

/**
 * サービス層での例外を表すクラス
 */
public class ServiceException extends ChunkDiscoveryException {
    
    public ServiceException(String message) {
        super(message);
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ServiceException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}