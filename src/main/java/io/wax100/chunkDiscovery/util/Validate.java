package io.wax100.chunkDiscovery.util;

/**
 * 引数検証ユーティリティクラス
 */
public class Validate {
    
    /**
     * オブジェクトがnullでないことを検証
     * @param obj 検証対象オブジェクト
     * @param message エラーメッセージ
     * @param <T> オブジェクトの型
     * @return 検証済みオブジェクト
     * @throws IllegalArgumentException nullの場合
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }
    
    /**
     * 文字列が空でないことを検証
     * @param str 検証対象文字列
     * @param message エラーメッセージ
     * @return 検証済み文字列
     * @throws IllegalArgumentException null或いは空文字の場合
     */
    public static String requireNonEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
    
    /**
     * 数値が範囲内にあることを検証
     * @param value 検証対象値
     * @param min 最小値（含む）
     * @param max 最大値（含む）
     * @param message エラーメッセージ
     * @return 検証済み値
     * @throws IllegalArgumentException 範囲外の場合
     */
    public static int requireInRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message + " (値: " + value + ", 範囲: " + min + "-" + max + ")");
        }
        return value;
    }
    
    /**
     * 数値が範囲内にあることを検証（double版）
     * @param value 検証対象値
     * @param min 最小値（含む）
     * @param max 最大値（含む）
     * @param message エラーメッセージ
     * @return 検証済み値
     * @throws IllegalArgumentException 範囲外の場合
     */
    public static double requireInRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message + " (値: " + value + ", 範囲: " + min + "-" + max + ")");
        }
        return value;
    }
    
    /**
     * 数値が正の値であることを検証
     * @param value 検証対象値
     * @param message エラーメッセージ
     * @return 検証済み値
     * @throws IllegalArgumentException 負の値または0の場合
     */
    public static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message + " (値: " + value + ")");
        }
        return value;
    }
    
    /**
     * 数値が非負の値であることを検証
     * @param value 検証対象値
     * @param message エラーメッセージ
     * @return 検証済み値
     * @throws IllegalArgumentException 負の値の場合
     */
    public static int requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message + " (値: " + value + ")");
        }
        return value;
    }
    
    /**
     * 配列が空でないことを検証
     * @param array 検証対象配列
     * @param message エラーメッセージ
     * @param <T> 配列の型
     * @return 検証済み配列
     * @throws IllegalArgumentException null或いは空配列の場合
     */
    public static <T> T[] requireNonEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }
    
    /**
     * 条件が真であることを検証
     * @param condition 検証対象条件
     * @param message エラーメッセージ
     * @throws IllegalArgumentException 条件が偽の場合
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}