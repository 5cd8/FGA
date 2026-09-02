package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CardTypePatternPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.FieldSlot
import io.github.fate_grand_automata.scripts.models.NPUsage
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.ServantPriorityPerWave
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.fate_grand_automata.scripts.modules.ApplyBraveChains
import io.github.fate_grand_automata.scripts.modules.CardPicker
import io.github.fate_grand_automata.scripts.modules.CardTypePatternSelector
import io.github.fate_grand_automata.scripts.modules.FaceCardPriority
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test

class CardPickerTest {
    private fun cardPicker(
        cardTypePatternPriority: String,
        braveChainsPerWave: List<BraveChainEnum> = emptyList(),
        rearrangeCardsPerWave: List<Boolean> = emptyList(),
        servantPriority: ServantPriorityPerWave? = null
    ): CardPicker {
        val battleConfig = mockk<IBattleConfig>()
        every { battleConfig.cardTypePatternPriority } returns CardTypePatternPriorityPerWave.of(cardTypePatternPriority)
        every { battleConfig.cardPriority } returns CardPriorityPerWave.default
        every { battleConfig.braveChains } returns braveChainsPerWave
        every { battleConfig.rearrangeCards } returns rearrangeCardsPerWave

        return CardPicker(
            cardTypePatternSelector = CardTypePatternSelector(),
            faceCardPriority = FaceCardPriority(CardPriorityPerWave.default, servantPriority),
            braveChains = ApplyBraveChains(),
            battleConfig = battleConfig
        )
    }

    @Test
    fun skipsBraveChainRearrangeWhenPatternMatches() {
        val picker = cardPicker(
            cardTypePatternPriority = "BQA",
            braveChainsPerWave = listOf(BraveChainEnum.None),
            rearrangeCardsPerWave = listOf(true)
        )

        val result = picker.pick(FaceCardPriorityTest.lineup1, NPUsage.none, stage = 0)

        // rearrange=true would swap positions 2 and 3 (E, C) if ApplyBraveChains ran;
        // seeing them unswapped confirms it never did for this pattern-matched turn.
        assertThat(result.take(3)).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.C)
    }

    @Test
    fun ignoresServantPriorityWhenPatternMatches() {
        val cardFromTeamB = ParsedCard(CommandCard.Face.A, TeamSlot.B, FieldSlot.B, CardTypeEnum.Buster)
        val cardFromTeamA = ParsedCard(CommandCard.Face.B, TeamSlot.A, FieldSlot.A, CardTypeEnum.Buster)

        // ServantPriorityPerWave.default ranks TeamSlot.A above TeamSlot.B; a pattern match
        // must ignore that and keep the hand's own dealt order (TeamB first) as the tiebreak.
        val picker = cardPicker(
            cardTypePatternPriority = "BBB",
            servantPriority = ServantPriorityPerWave.default
        )

        // 2 NPs reduce "BBB" to a single required Buster (CardTypePatternSelector's contract).
        val result = picker.pick(
            cards = listOf(cardFromTeamB, cardFromTeamA),
            npUsage = NPUsage(setOf(CommandCard.NP.A, CommandCard.NP.B), 0),
            stage = 0
        )

        assertThat(result.take(1)).containsExactly(CommandCard.Face.A)
    }

    @Test
    fun fallsBackToPriorityAndBraveChainPipelineWhenNoPatternIsConfigured() {
        val picker = cardPicker(cardTypePatternPriority = "")

        val result = picker.pick(FaceCardPriorityTest.lineup1, NPUsage.none, stage = 0)

        // Same expectation as FaceCardPriorityTest.defaultPriority: no patterns configured
        // means the pre-existing CardPriority pipeline decides, unchanged.
        assertThat(result).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.B, CommandCard.Face.C, CommandCard.Face.D)
    }

    @Test
    fun fallsBackToPriorityAndBraveChainPipelineWhenThePatternCannotBeSatisfied() {
        // lineup1 has only 1 Buster card, so "BBB" is never satisfiable.
        val picker = cardPicker(cardTypePatternPriority = "BBB")

        val result = picker.pick(FaceCardPriorityTest.lineup1, NPUsage.none, stage = 0)

        assertThat(result).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.B, CommandCard.Face.C, CommandCard.Face.D)
    }

    @Test
    fun picksThePatternConfiguredForTheCurrentWave() {
        // Wave 1 (index 0) has an unsatisfiable pattern; wave 2 (index 1) has one lineup1 can match.
        val picker = cardPicker(cardTypePatternPriority = "BBB\nBQA")

        val wave1Result = picker.pick(FaceCardPriorityTest.lineup1, NPUsage.none, stage = 0)
        val wave2Result = picker.pick(FaceCardPriorityTest.lineup1, NPUsage.none, stage = 1)

        assertThat(wave1Result).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.B, CommandCard.Face.C, CommandCard.Face.D)
        assertThat(wave2Result.take(3)).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.C)
    }
}
