package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.fate_grand_automata.scripts.models.PreferredCommandCodePerWave
import kotlin.test.Test

class PreferredCommandCodePerWaveTest {
    @Test
    fun returnsTheNameConfiguredForEachWave() {
        val perWave = PreferredCommandCodePerWave.of("np20\nrainbow_apple")

        assertThat(perWave.atWave(0)).isEqualTo("np20")
        assertThat(perWave.atWave(1)).isEqualTo("rainbow_apple")
    }

    @Test
    fun clampsAWaveNumberPastTheLastConfiguredWaveToTheLastWave() {
        val perWave = PreferredCommandCodePerWave.of("np20\nrainbow_apple")

        assertThat(perWave.atWave(99)).isEqualTo("rainbow_apple")
    }

    @Test
    fun blankConfigurationLeavesEveryWaveWithNoPreferredCode() {
        val perWave = PreferredCommandCodePerWave.of("")

        assertThat(perWave.atWave(0)).isEqualTo("")
        assertThat(perWave.atWave(99)).isEqualTo("")
    }
}
