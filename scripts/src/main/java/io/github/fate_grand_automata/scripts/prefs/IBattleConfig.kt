package io.github.fate_grand_automata.scripts.prefs

import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.enums.MaterialEnum
import io.github.fate_grand_automata.scripts.enums.ShuffleCardsEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CardTypePatternPriorityPerWave
import io.github.fate_grand_automata.scripts.models.PreferredCommandCodePerWave
import io.github.fate_grand_automata.scripts.models.ServantPriorityPerWave
import io.github.fate_grand_automata.scripts.models.ServantSpamConfig

interface IBattleConfig {
    val id: String
    var name: String
    var skillCommand: String
    var cardPriority: CardPriorityPerWave
    var cardTypePatternPriority: CardTypePatternPriorityPerWave

    /**
     * Name of a user-provided Command Code (指令紋章) image under the `command_code`
     * support-image folder (same mechanism as Servant/CE/Friend images, see
     * [io.github.fate_grand_automata.SupportImageKind]), one per wave. Blank for a wave
     * means the feature is off for that wave. When set, a face card matching this image on
     * [io.github.fate_grand_automata.scripts.locations.AttackScreenLocations.commandCodeRegion]
     * is preferred over an otherwise-tied same-type/affinity card by
     * [io.github.fate_grand_automata.scripts.modules.FaceCardPriority] and
     * [io.github.fate_grand_automata.scripts.modules.CardTypePatternSelector].
     */
    var preferredCommandCode: PreferredCommandCodePerWave

    val useServantPriority: Boolean
    val servantPriority: ServantPriorityPerWave
    val rearrangeCards: List<Boolean>
    val braveChains: List<BraveChainEnum>
    val party: Int
    val materials: Set<MaterialEnum>
    val support: ISupportPreferences
    val shuffleCards: ShuffleCardsEnum
    val shuffleCardsWave: Int

    var spam: List<ServantSpamConfig>
    val autoChooseTarget: Boolean

    val server: GameServer?

    val addRaidTurnDelay: Boolean
    val raidTurnDelaySeconds : Int

    fun export(): Map<String, *>

    fun import(map: Map<String, *>)
}