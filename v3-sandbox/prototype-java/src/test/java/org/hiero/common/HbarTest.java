package org.hiero.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HbarTest {

    @Test
    void hbarToTinybars() {
        // given
        final Hbar oneHbar = Hbar.of(1L);

        // when
        final long tinybars = oneHbar.toTinybars();

        // then
        assertEquals(100_000_000L, tinybars);
    }

    @Test
    void convertBetweenUnitsLossless() {
        // given
        final Hbar oneHbar = Hbar.of(1L);

        // when
        final Hbar inMicrobar = oneHbar.to(HbarUnit.MICROBAR);

        // then
        assertEquals(1_000_000L, inMicrobar.amount());
        assertEquals(HbarUnit.MICROBAR, inMicrobar.unit());
    }
}
