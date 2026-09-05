package io.github.fate_grand_automata.scripts.locations

import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.lib_automata.Location
import io.github.lib_automata.Region
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class AttackScreenLocations @Inject constructor(
    scriptAreaTransforms: IScriptAreaTransforms
) : IScriptAreaTransforms by scriptAreaTransforms {
    private fun clickLocation(card: CommandCard.Face) = when (card) {
        CommandCard.Face.A -> -980
        CommandCard.Face.B -> -530
        CommandCard.Face.C -> 20
        CommandCard.Face.D -> 520
        CommandCard.Face.E -> 1070
    }.let { x -> Location(x, 1000) }

    fun clickLocation(card: CommandCard) = when (card) {
        is CommandCard.Face -> clickLocation(card)
        CommandCard.NP.A -> Location(-280, 220)
        CommandCard.NP.B -> Location(20, 400)
        CommandCard.NP.C -> Location(460, 400)
    }.xFromCenter()

    private val faceCardDeltaY =
        Location(0, if (gameServer == GameServer.Cn && isWide) -42 else 0)

    fun affinityRegion(card: CommandCard.Face) = when (card) {
        CommandCard.Face.A -> -985
        CommandCard.Face.B -> -470
        CommandCard.Face.C -> 41
        CommandCard.Face.D -> 554
        CommandCard.Face.E -> 1068
    }.let { x -> Region(x, 590, 250, 260) + faceCardDeltaY }.xFromCenter()

    // Issue #5 field measurement (10 badge/type-banner pairs across 2 different servant
    // lineups and Command Code sets): the badge's center sits a steady ~260 script-px above
    // the *matched* position of that card's own Buster/Arts/Quick banner -- steady across
    // both lineups despite the two banners in each lineup sitting at visibly different
    // heights on screen, because both the badge and the banner are part of the same card
    // sprite and float up/down together. A fixed y offset from the card's nominal (script
    // table) position does NOT hold anywhere near this well: two badges in the very same
    // screenshot measured up to ~90px apart. See commandCodeRegion below.
    private val commandCodeBadgeYOffsetFromTypeText = 260

    // Search box around the anchored center, not a tight crop of the badge itself -- covers
    // measured badge diameters (~180-220 script-px) plus margin for this offset's own
    // measured spread (236-269 script-px across those 10 pairs) and ordinary per-frame
    // jitter. The reference *template* image should still be cropped tight to the badge
    // (wiki/scripts/Support-Image-Maker.md) -- only this search box benefits from slack, the
    // same way the bundled Weak/Resist icons affinityRegion searches for are themselves
    // small, tightly-cropped templates within a larger search area.
    private val commandCodeSearchWidth = 260
    private val commandCodeSearchHeight = 240

    /**
     * Search region for a Command Code (指令紋章) badge on [card], anchored to
     * [typeTextMatch] -- the actual matched [Region] of that card's Buster/Arts/Quick
     * banner *this frame* (`typeRegion(card).find(images[...])?.region`), not a fixed
     * offset from the card's own script-table position. Face cards float up/down
     * continuously and independently on the Attack screen, so no fixed y offset survives
     * that motion (see [commandCodeBadgeYOffsetFromTypeText]'s comment) -- but the banner
     * position is already something CardParser reliably re-locates every turn (that's how
     * card type is read at all), so anchoring to it directly compensates for that
     * same-frame drift instead of needing a search box wide enough to blindly tolerate it.
     * x doesn't need this treatment: [affinityRegion]'s x table already lines up with the
     * badge horizontally, within a few px (Issue #5, https://github.com/5cd8/FGA/issues/5).
     * Not yet cross-checked on a non-wide (16:9/18:9) device; see [servantMatchRegion]'s
     * isWide precedent if that turns out to be necessary.
     */
    fun commandCodeRegion(card: CommandCard.Face, typeTextMatch: Region): Region {
        val centerX = affinityRegion(card).let { it.x + it.width / 2 }
        val centerY = typeTextMatch.y - commandCodeBadgeYOffsetFromTypeText

        return Region(
            centerX - commandCodeSearchWidth / 2,
            centerY - commandCodeSearchHeight / 2,
            commandCodeSearchWidth,
            commandCodeSearchHeight
        )
    }

    fun typeRegion(card: CommandCard.Face) = when (card) {
        CommandCard.Face.A -> -1280
        CommandCard.Face.B -> -768
        CommandCard.Face.C -> -256
        CommandCard.Face.D -> 256
        CommandCard.Face.E -> 768
    }.let { x -> Region(x, 1060, 512, 200) + faceCardDeltaY }.xFromCenter()

    fun servantMatchRegion(card: CommandCard.Face) = when (card) {
        CommandCard.Face.A -> -1174
        CommandCard.Face.B -> -660
        CommandCard.Face.C -> -150
        CommandCard.Face.D -> 364
        CommandCard.Face.E -> 880
    }.let { x -> Region(x - 100, 700, 500, 400) + faceCardDeltaY }.xFromCenter()

    fun supportCheckRegion(card: CommandCard.Face) =
        affinityRegion(card) + Location(-50, 100)

    val backClick =
        (if (isWide)
            Location(-325, 1310)
        else Location(-160, 1370))
            .xFromRight()
}