package io.wax100.chunkDiscovery.initializer;

import org.bukkit.configuration.Configuration;
import java.util.logging.Logger;

/**
 * 設定ファイルのバリデーションを担当するクラス
 */
public class ConfigValidator {
    private final Configuration config;
    private final Logger logger;
    
    public ConfigValidator(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }
    
    /**
     * 設定値の全体的なバリデーション
     */
    public boolean validate() {
        return validateBorderSettings() && validateDatabaseSettings();
    }
    
    private boolean validateBorderSettings() {
        double initSize = config.getDouble("border.initial_size", 100.0);
        double perChunk = config.getDouble("border.expansion_per_chunk", 1.0);
        
        if (!isValidBorderSize(initSize)) {
            logger.severe("border.initial_size は 0 以上 60000000 以下である必要があります: " + initSize);
            return false;
        }
        
        if (!isValidExpansionRate(perChunk)) {
            logger.severe("border.expansion_per_chunk は 0 以上 1000 以下である必要があります: " + perChunk);
            return false;
        }
        
        return true;
    }
    
    private boolean validateDatabaseSettings() {
        String host = config.getString("db.host");
        int port = config.getInt("db.port", 3306);
        String dbName = config.getString("db.name");
        String user = config.getString("db.user");
        
        if (!isValidHost(host)) {
            logger.severe("db.host が設定されていません。");
            return false;
        }
        
        if (!isValidPort(port)) {
            logger.severe("db.port は 1 以上 65535 以下である必要があります: " + port);
            return false;
        }
        
        if (!isValidDatabaseName(dbName)) {
            logger.severe("db.name が設定されていません。");
            return false;
        }
        
        if (!isValidUsername(user)) {
            logger.severe("db.user が設定されていません。");
            return false;
        }
        
        return true;
    }
    
    private boolean isValidBorderSize(double size) {
        return size >= io.wax100.chunkDiscovery.Constants.Validation.MIN_BORDER_SIZE 
            && size <= io.wax100.chunkDiscovery.Constants.Validation.MAX_BORDER_SIZE;
    }
    
    private boolean isValidExpansionRate(double rate) {
        return rate >= io.wax100.chunkDiscovery.Constants.Validation.MIN_EXPANSION_PER_CHUNK 
            && rate <= io.wax100.chunkDiscovery.Constants.Validation.MAX_EXPANSION_PER_CHUNK;
    }
    
    private boolean isValidHost(String host) {
        return host != null && !host.trim().isEmpty();
    }
    
    private boolean isValidPort(int port) {
        return port >= io.wax100.chunkDiscovery.Constants.Validation.MIN_PORT 
            && port <= io.wax100.chunkDiscovery.Constants.Validation.MAX_PORT;
    }
    
    private boolean isValidDatabaseName(String dbName) {
        return dbName != null && !dbName.trim().isEmpty();
    }
    
    private boolean isValidUsername(String user) {
        return user != null && !user.trim().isEmpty();
    }
}