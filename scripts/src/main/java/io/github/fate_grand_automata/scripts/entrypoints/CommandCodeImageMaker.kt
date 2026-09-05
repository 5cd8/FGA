package io.github.fate_grand_automata.scripts.entrypoints

import io.github.fate_grand_automata.IStorageProvider
import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.lib_automata.EntryPoint
import io.github.lib_automata.ExitManager
import io.github.lib_automata.dagger.ScriptScope
import java.io.File
import javax.inject.Inject

/**
 * Captures the whole current screen (in color -- Command Code badges need color to
 * discriminate from one another, see CardParser.commandCodeMatchScore's comment) and hands it
 * off to the Command Code namer/cropper dialog for the user to drag a crop box over one badge.
 *
 * This mirrors [SupportImageMaker]'s own approach -- extracting straight from
 * [io.github.lib_automata.AutomataApi.getPattern]'s live, already-matching-space screenshot --
 * rather than an external tool re-deriving FGA's script(1440p)->image(720p) scaling against a
 * separately-saved raw screenshot file. Issue #5's investigation found that external
 * re-derivation unreliable to keep byte-for-byte consistent with the real runtime capture
 * pipeline (MediaProjectionScreenshotService captures at an already-downscaled resolution, not
 * the device's native one -- see ScreenshotServiceHolder.prepareScreenshotService), even after
 * the math driving it was independently confirmed correct against FgoGameAreaManager. Cropping
 * from the exact same live Pattern [AutomataApi.findAll] itself matches against removes that
 * whole class of risk by construction, the same way it already does for the bundled
 * servant/CE/friend-name templates SupportImageMaker produces.
 */
@ScriptScope
class CommandCodeImageMaker @Inject constructor(
    private val storageProvider: IStorageProvider,
    exitManager: ExitManager,
    api: IFgoAutomataApi
) : EntryPoint(exitManager), IFgoAutomataApi by api {
    companion object {
        fun getCaptureImgPath(dir: File): File =
            File(dir, "command_code_capture.png")
    }

    class ExitException : Exception()

    override fun script(): Nothing {
        val capture = useColor { locations.scriptArea.getPattern() }

        capture.use {
            getCaptureImgPath(storageProvider.supportImageTempDir)
                .outputStream()
                .use { stream -> it.save(stream) }
        }

        throw ExitException()
    }
}
