package org.hiero.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void parseShardRealmNum() {
        // given
        final String input = "0.0.1234";

        // when
        final AccountId id = AccountId.fromString(input);

        // then
        assertEquals(0L, id.shard());
        assertEquals(0L, id.realm());
        assertEquals(1234L, id.num());
        assertEquals("", id.checksum());
        assertEquals("0.0.1234", id.toString());
    }

    @Test
    void parseWithChecksum() {
        // given
        final String input = "0.0.1234-abcde";

        // when
        final AccountId id = AccountId.fromString(input);

        // then
        assertEquals("abcde", id.checksum());
        assertEquals("0.0.1234-abcde", id.toStringWithChecksum());
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AccountId(-1L, 0L, 0L));
    }

    @Test
    void rejectsInvalidString() {
        assertThrows(IllegalArgumentException.class, () -> AccountId.fromString("not-an-id"));
    }
}
