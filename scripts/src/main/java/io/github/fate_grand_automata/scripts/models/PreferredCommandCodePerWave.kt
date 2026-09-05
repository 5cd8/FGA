package io.github.fate_grand_automata.scripts.models

/**
 * Per-wave wrapper around a Command Code image name, mirroring
 * [CardTypePatternPriorityPerWave]'s shape: one line per wave, a blank line meaning "no
 * preferred Command Code for that wave".
 */
class PreferredCommandCodePerWave private constructor(
    private val namesPerWave: List<String>
) : List<String> by namesPerWave {
    fun atWave(wave: Int) =
        namesPerWave[wave.coerceIn(namesPerWave.indices)]

    override fun toString() =
        namesPerWave.joinToString(waveSeparator)

    companion object {
        private const val waveSeparator = "\n"

        fun from(namesPerWave: List<String>) = PreferredCommandCodePerWave(namesPerWave)

        // No isBlank() special case, mirroring CardTypePatternPriorityPerWave.of():
        // "".split(waveSeparator) already yields a single blank element, and a blank
        // element is already the meaningful "no preferred code for this wave" state,
        // not a parse error.
        fun of(names: String): PreferredCommandCodePerWave =
            PreferredCommandCodePerWave(names.split(waveSeparator))
    }
}
