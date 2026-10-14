package org.hiero.sdk.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link UnsignedValidator} covering all unsigned integer type boundaries.
 *
 * <p>Each test follows the Given-When-Then pattern as defined in the
 * <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-best-practices-java.md">
 * Java API Implementation Guideline</a>.
 */
class UnsignedValidatorTest {



    @Test
    void testRequireUint8AcceptsZero() {

        final int value = 0;

  
        final int result = UnsignedValidator.requireUint8(value, "testField");

        assertEquals(0, result, "uint8 must accept 0");
    }

    @Test
    void testRequireUint8AcceptsMaxBoundary() {
        final int value = 255;

        final int result = UnsignedValidator.requireUint8(value, "testField");

        assertEquals(255, result, "uint8 must accept 255");
    }

    @Test
    void testRequireUint8AcceptsMidRangeValue() {
      
        final int value = 128;

        final int result = UnsignedValidator.requireUint8(value, "testField");

        
        assertEquals(128, result, "uint8 must accept values within range");
    }


    @Test
    void testRequireUint8RejectsNegativeValue() {
        final int value = -1;

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint8(value, "testField"),
                "uint8 must reject negative values"
        );
        assertEquals(
                "testField must be in the range 0 to 255 (uint8), but was: -1",
                exception.getMessage()
        );
    }

    @Test
    void testRequireUint8RejectsValueAboveMax() {
       
        final int value = 256;

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint8(value, "testField"),
                "uint8 must reject values above 255"
        );
        assertEquals(
                "testField must be in the range 0 to 255 (uint8), but was: 256",
                exception.getMessage()
        );
    }

    @Test
    void testRequireUint8RejectsLargeNegativeValue() {
   
        final int value = Integer.MIN_VALUE;

        
        assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint8(value, "testField"),
                "uint8 must reject Integer.MIN_VALUE"
        );
    }


    @Test
    void testRequireUint16AcceptsZero() {
        final int value = 0;

        final int result = UnsignedValidator.requireUint16(value, "port");

        assertEquals(0, result, "uint16 must accept 0");
    }

    @Test
    void testRequireUint16AcceptsMaxBoundary() {
       
        final int value = 65535;
        final int result = UnsignedValidator.requireUint16(value, "port");

        assertEquals(65535, result, "uint16 must accept 65535");
    }

    @Test
    void testRequireUint16AcceptsTypicalPort() {
       
        final int value = 50211;

      
        final int result = UnsignedValidator.requireUint16(value, "port");

       
        assertEquals(50211, result, "uint16 must accept typical port values");
    }

   

    @Test
    void testRequireUint16RejectsNegativeValue() {
       
        final int value = -1;

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint16(value, "port"),
                "uint16 must reject negative values"
        );
        assertEquals(
                "port must be in the range 0 to 65535 (uint16), but was: -1",
                exception.getMessage()
        );
    }

    @Test
    void testRequireUint16RejectsValueAboveMax() {
    
        final int value = 65536;

       
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint16(value, "port"),
                "uint16 must reject values above 65535"
        );
        assertEquals(
                "port must be in the range 0 to 65535 (uint16), but was: 65536",
                exception.getMessage()
        );
    }

   

    @Test
    void testRequireUint64AcceptsZero() {
       
        final long value = 0L;

      
        final long result = UnsignedValidator.requireUint64(value, "shard");

       
        assertEquals(0L, result, "uint64 must accept 0");
    }

    @Test
    void testRequireUint64AcceptsMaxBoundary() {
     
        final long value = Long.MAX_VALUE;

       
        final long result = UnsignedValidator.requireUint64(value, "shard");

      
        assertEquals(Long.MAX_VALUE, result, "uint64 must accept Long.MAX_VALUE");
    }

    @Test
    void testRequireUint64AcceptsTypicalValue() {
      
        final long value = 1_000_000L;

      
        final long result = UnsignedValidator.requireUint64(value, "realm");

        
        assertEquals(1_000_000L, result, "uint64 must accept typical positive values");
    }


    @Test
    void testRequireUint64RejectsNegativeValue() {
        final long value = -1L;

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint64(value, "shard"),
                "uint64 must reject negative values"
        );
        assertEquals(
                "shard must be non-negative (uint64), but was: -1",
                exception.getMessage()
        );
    }

    @Test
    void testRequireUint64RejectsMinLongValue() {
        final long value = Long.MIN_VALUE;

        assertThrows(
                IllegalArgumentException.class,
                () -> UnsignedValidator.requireUint64(value, "shard"),
                "uint64 must reject Long.MIN_VALUE"
        );
    }


    @Test
    void testRequireUint8ReturnsValidatedValue() {
        
        final int value = 42;

        final int result = UnsignedValidator.requireUint8(value, "field");

        assertEquals(42, result, "requireUint8 must return the validated value for inline use");
    }

    @Test
    void testRequireUint16ReturnsValidatedValue() {
   
        final int value = 8080;
       
        final int result = UnsignedValidator.requireUint16(value, "field");

        assertEquals(8080, result, "requireUint16 must return the validated value for inline use");
    }

    @Test
    void testRequireUint64ReturnsValidatedValue() {
       
        final long value = 999L;
        
        final long result = UnsignedValidator.requireUint64(value, "field");
        
        assertEquals(999L, result, "requireUint64 must return the validated value for inline use");
    }
}
