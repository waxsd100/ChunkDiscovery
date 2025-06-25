package io.wax100.chunkDiscovery.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidateTest {

    @Test
    void testRequireNonNull_ValidObject() {
        String validString = "test";
        String result = Validate.requireNonNull(validString, "Should not throw");
        assertEquals("test", result);
    }

    @Test
    void testRequireNonNull_NullObject() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonNull(null, "Object cannot be null");
        });
    }

    @Test
    void testRequireNonEmpty_ValidString() {
        String validString = "test";
        String result = Validate.requireNonEmpty(validString, "Should not throw");
        assertEquals("test", result);
    }

    @Test
    void testRequireNonEmpty_NullString() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonEmpty((String) null, "String cannot be null");
        });
    }

    @Test
    void testRequireNonEmpty_EmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonEmpty("", "String cannot be empty");
        });
    }

    @Test
    void testRequireNonEmpty_WhitespaceString() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonEmpty("   ", "String cannot be whitespace");
        });
    }

    @Test
    void testRequireInRange_ValidInt() {
        int result = Validate.requireInRange(5, 1, 10, "Should not throw");
        assertEquals(5, result);
    }

    @Test
    void testRequireInRange_IntTooLow() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireInRange(0, 1, 10, "Value out of range");
        });
    }

    @Test
    void testRequireInRange_IntTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireInRange(11, 1, 10, "Value out of range");
        });
    }

    @Test
    void testRequireInRange_ValidDouble() {
        double result = Validate.requireInRange(5.5, 1.0, 10.0, "Should not throw");
        assertEquals(5.5, result);
    }

    @Test
    void testRequirePositive_ValidInt() {
        int result = Validate.requirePositive(5, "Should not throw");
        assertEquals(5, result);
    }

    @Test
    void testRequirePositive_Zero() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requirePositive(0, "Value must be positive");
        });
    }

    @Test
    void testRequirePositive_NegativeInt() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requirePositive(-1, "Value must be positive");
        });
    }

    @Test
    void testRequireNonNegative_ValidInt() {
        int result = Validate.requireNonNegative(5, "Should not throw");
        assertEquals(5, result);
    }

    @Test
    void testRequireNonNegative_Zero() {
        int result = Validate.requireNonNegative(0, "Should not throw");
        assertEquals(0, result);
    }

    @Test
    void testRequireNonNegative_NegativeInt() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonNegative(-1, "Value must be non-negative");
        });
    }

    @Test
    void testRequireNonEmptyArray_ValidArray() {
        String[] validArray = {"test"};
        String[] result = Validate.requireNonEmpty(validArray, "Should not throw");
        assertArrayEquals(new String[]{"test"}, result);
    }

    @Test
    void testRequireNonEmptyArray_NullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonEmpty((String[]) null, "Array cannot be null");
        });
    }

    @Test
    void testRequireNonEmptyArray_EmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireNonEmpty(new String[0], "Array cannot be empty");
        });
    }

    @Test
    void testRequireTrue_ValidCondition() {
        assertDoesNotThrow(() -> {
            Validate.requireTrue(true, "Should not throw");
        });
    }

    @Test
    void testRequireTrue_InvalidCondition() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.requireTrue(false, "Condition must be true");
        });
    }
}