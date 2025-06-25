package io.wax100.chunkDiscovery.exception;

/**
 * 設定ファイルの読み込みや検証で発生する例外
 */
public class ConfigurationException extends ChunkDiscoveryException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}