package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoundingModeTest {

    @Test
    void exposesExactlyTheSupportedModes() {
        assertThat(RoundingMode.values())
                .containsExactly(RoundingMode.HALF_UP, RoundingMode.HALF_EVEN);
    }
}
