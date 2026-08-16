package com.gullycricket.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @Test
    void canonicalizesWhitespaceAndCase() {
        assertThat(NameNormalizer.normalize("  Virat   Kohli ")).isEqualTo("virat kohli");
    }
}
