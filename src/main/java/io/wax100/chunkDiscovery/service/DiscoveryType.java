package io.wax100.chunkDiscovery.service;

/**
 * チャンク発見のタイプを表すenum
 */
public enum DiscoveryType {
    /** 世界初発見 */
    WORLD_FIRST,
    
    /** 個人初発見 */
    PERSONAL_FIRST,
    
    /** 既発見（報酬なし） */
    ALREADY_DISCOVERED
}