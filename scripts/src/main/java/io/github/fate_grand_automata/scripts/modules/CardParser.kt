package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.SupportImageKind
import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.ScriptLog
import io.github.fate_grand_automata.scripts.ScriptNotify
import io.github.fate_grand_automata.scripts.enums.CardAffinityEnum
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class CardParser @Inject constructor(
    api: IFgoAutomataApi,
    private val servantTracker: ServantTracker,
    private val battleConfig: IBattleConfig
) : IFgoAutomataApi by api {

    private fun CommandCard.Face.affinity(): CardAffinityEnum {
        val region = locations.attack.affinityRegion(this)

        if (images[Images.Weak] in region) {
            return CardAffinityEnum.Weak
        }

        if (images[Images.Resist] in region) {
            return CardAffinityEnum.Resist
        }

        return CardAffinityEnum.Normal
    }

    private fun CommandCard.Face.isStunned(): Boolean {
        val stunRegion = locations.attack.typeRegion(this).copy(
            y = 930,
            width = 248,
            height = 188
        )

        return listOf(
            images[Images.Stun],
            images[Images.Immobilized],
            images[Images.StunBuster],
            images[Images.StunArts],
            images[Images.StunQuick],
        ) in stunRegion
    }

    private fun CommandCard.Face.type(): CardTypeEnum {
        val region = locations.attack.typeRegion(this)

        if (images[Images.Buster] in region) {
            return CardTypeEnum.Buster
        }

        if (images[Images.Arts] in region) {
            return CardTypeEnum.Arts
        }

        if (images[Images.Quick] in region) {
            return CardTypeEnum.Quick
        }

        return CardTypeEnum.Unknown
    }

    fun parse(stage: Int): List<ParsedCard> {
        val cardsGroupedByServant = servantTracker.faceCardsGroupedByServant()

        val cards = CommandCard.Face.list
            .map {
                val stunned = it.isStunned()
                val type = if (stunned)
                    CardTypeEnum.Unknown
                else it.type()
                val affinity = if (type == CardTypeEnum.Unknown)
                    CardAffinityEnum.Normal // Couldn't detect card type, so don't care about affinity
                else it.affinity()

                val servant = cardsGroupedByServant
                    .filterValues { cards -> it in cards }
                    .keys
                    .firstOrNull()
                    ?: TeamSlot.Unknown

                val fieldSlot = servantTracker.deployed
                    .entries
                    .firstOrNull { (_, teamSlot) -> teamSlot == servant }
                    ?.key

                ParsedCard(
                    card = it,
                    isStunned = stunned,
                    type = type,
                    affinity = affinity,
                    servant = servant,
                    fieldSlot = fieldSlot
                )
            }

        var unknownCardTypes = false
        var unknownServants = false
        val failedToDetermine = cards
            .filter {
                when {
                    it.isStunned -> false
                    it.type == CardTypeEnum.Unknown -> {
                        unknownCardTypes = true
                        true
                    }

                    it.servant is TeamSlot.Unknown && !prefs.skipServantFaceCardCheck -> {
                        unknownServants = true
                        true
                    }

                    else -> false
                }
            }
            .map { it.card }

        if (failedToDetermine.isNotEmpty()) {
            messages.notify(
                ScriptNotify.FailedToDetermineCards(failedToDetermine, unknownCardTypes, unknownServants)
            )
        }

        return cards
    }

    // Region.exists()'s default (Fine-Tune's minSimilarity, normally 80%) is tuned for
    // static template art. Command Code badges carry a shine/particle animation on top of
    // the artwork itself, which -- on top of the face cards' own float animation already
    // accounted for in commandCodeRegion -- makes the correlation score drift turn to turn;
    // Issue #5 field testing saw the same configured code sometimes match and sometimes not
    // against the same physical card. ServantSelection.kt's `similarity = 0.68` override for
    // the (similarly small, similarly animated) SkillTen badge is the existing precedent for
    // loosening this per-feature rather than globally. Badges are larger/more visually
    // distinct than that skill badge, so this starts higher (0.75); adjust from real match
    // data (temporarily log the raw score before the threshold check below) rather than by
    // feel if it needs retuning.
    private val preferredCommandCodeSimilarity = 0.75

    // Diagnostic floor for the score log below -- NOT 0.0. AutomataApi.find()/findAll()
    // don't stop at the first candidate; RealImageMatcher.findAll() drains the underlying
    // DroidCvPattern.findMatches() sequence with .toList() regardless of how many results
    // the caller actually wants, and that sequence keeps iteratively finding-then-flood-
    // filling the next-best peak in the search region until a score drops below the given
    // similarity. A near-zero floor forced that loop through many more peaks than intended
    // across commandCodeRegion's widened area -- confirmed by this actually hanging AutoBattle
    // at the Attack screen on a real device. 0.5 keeps the same "see scores below the real
    // 0.75 threshold" diagnostic value while stopping the search after at most a couple of
    // peaks, the same way a normal (default ~0.8) similarity search would.
    private val commandCodeDiagnosticFloor = 0.5

    // Every Command Code badge shares the same decorative white "wing" frame -- only the
    // small emblem in the center actually differs between codes. In grayscale that shared
    // frame dominates the correlation score, so a *different* code's badge can score higher
    // than the real one (confirmed on a real device: b_crit20's actual card scored 0.65 while
    // two unrelated codes scored 0.75+ against its own template). Command Code artwork is
    // otherwise strongly color-differentiated (cyan/pink vs teal/gold vs grey), so matching
    // in color -- same mechanism ScreenshotDrops.kt already uses -- widens that gap back out.
    // Existing reference images stay valid either way: PNGs already carry full color,
    // useColor only changes how they (and the screenshot) get decoded for this
    // comparison. This runs as its OWN useColor { useSameSnapIn { ... } } in
    // applyCommandCodePreference below rather than sharing parse()'s cached screenshot:
    // ScreenshotManager.getScreenshot() returns whatever Pattern useSameSnapIn cached
    // regardless of the current useColor state, so nesting this inside parse()'s (grayscale)
    // snapshot fed a color template against a still-grayscale cached screenshot and crashed
    // OpenCV's matchTemplate on a channel-count mismatch (confirmed on a real device). Running
    // this only after that outer useSameSnapIn has already exited -- see Card.readCommandCards
    // -- forces a fresh, actually-color screenshot instead.
    //
    // [cardType] is parse()'s already-determined type for this card (grayscale pass), used
    // only to pick *which one* of Buster/Arts/Quick to re-search for here -- the match itself
    // has to be redone against this (color, fresh-screenshot) pass rather than reusing
    // parse()'s, so its position reflects this exact frame (AttackScreenLocations
    // .commandCodeRegion anchors to it; see that function's comment for why).
    private fun CommandCard.Face.commandCodeMatchScore(name: String, cardType: CardTypeEnum): Double {
        val typeImage = when (cardType) {
            CardTypeEnum.Buster -> Images.Buster
            CardTypeEnum.Arts -> Images.Arts
            CardTypeEnum.Quick -> Images.Quick
            CardTypeEnum.Unknown -> return 0.0
        }

        val typeTextMatch = locations.attack.typeRegion(this).find(images[typeImage])
            ?: return 0.0

        val region = locations.attack.commandCodeRegion(this, typeTextMatch.region)

        return images.loadSupportPattern(SupportImageKind.CommandCode, name)
            .mapNotNull { region.find(it, similarity = commandCodeDiagnosticFloor)?.score }
            .maxOrNull() ?: 0.0
    }

    /**
     * Fills in [ParsedCard.hasCommandCode] on [cards] against
     * [IBattleConfig.preferredCommandCode] at [stage]. Call this only after the
     * [io.github.lib_automata.ScreenshotManager.useSameSnapIn] block [parse] ran in has
     * already exited -- see the comment on [commandCodeMatchScore] for why sharing that
     * (grayscale) cached screenshot crashes.
     */
    fun applyCommandCodePreference(cards: List<ParsedCard>, stage: Int): List<ParsedCard> {
        val preferredCommandCodeName = battleConfig.preferredCommandCode.atWave(stage)

        if (preferredCommandCodeName.isBlank()) {
            return cards
        }

        // useColor before useSameSnapIn so the fresh screenshot useSameSnapIn caches for this
        // block is captured in color from the start (ScreenshotManager.snapshot() reads
        // ColorManager.isColor at capture time).
        val updated = useColor {
            useSameSnapIn {
                cards.map { card ->
                    // Stunned cards' type is already Unknown, so they can never win a
                    // same-type tiebreak -- there's nothing to gain by checking them.
                    if (card.isStunned) {
                        card
                    } else {
                        val score = card.card.commandCodeMatchScore(preferredCommandCodeName, card.type)
                        card.copy(hasCommandCode = score >= preferredCommandCodeSimilarity)
                    }
                }
            }
        }

        updated
            .filter { it.hasCommandCode }
            .map { it.card }
            .let { matched ->
                if (matched.isNotEmpty()) {
                    messages.log(ScriptLog.CommandCodesDetected(preferredCommandCodeName, matched))
                }
            }

        return updated
    }
}
