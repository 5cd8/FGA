package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.NPUsage
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

/**
 * Chooses the 3 face cards a turn taps. A type pattern that the hand can satisfy wins
 * outright; otherwise [FaceCardPriority] + [ApplyBraveChains] decide, unchanged from
 * before type patterns existed. A match skips both CardPriority-driven servant grouping
 * and brave chain rearrangement entirely: the user asked for an exact per-position type
 * order, and letting either system reorder it afterward could silently break that order.
 */
@ScriptScope
class CardPicker @Inject constructor(
    private val cardTypePatternSelector: CardTypePatternSelector,
    private val faceCardPriority: FaceCardPriority,
    private val braveChains: ApplyBraveChains,
    private val battleConfig: IBattleConfig
) {
    fun pick(
        cards: List<ParsedCard>,
        npUsage: NPUsage,
        stage: Int
    ): List<CommandCard.Face> {
        val typePatternMatch = cardTypePatternSelector.select(
            cards = cards,
            patterns = battleConfig.cardTypePatternPriority.atWave(stage),
            cardPriority = battleConfig.cardPriority,
            stage = stage,
            npUsage = npUsage
        )

        if (typePatternMatch != null) {
            return typePatternMatch.map { it.card }
        }

        fun <T> List<T>.inCurrentWave(default: T) =
            if (isNotEmpty())
                this[stage.coerceIn(indices)]
            else default

        val cardsOrderedByPriority = faceCardPriority.sort(cards, stage)

        return braveChains.pick(
            cards = cardsOrderedByPriority,
            npUsage = npUsage,
            braveChains = battleConfig.braveChains.inCurrentWave(BraveChainEnum.None),
            rearrange = battleConfig.rearrangeCards.inCurrentWave(false)
        ).map { it.card }
    }
}
