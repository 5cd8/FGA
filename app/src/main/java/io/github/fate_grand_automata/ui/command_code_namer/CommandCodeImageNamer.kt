package io.github.fate_grand_automata.ui.command_code_namer

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.SupportImageKind
import io.github.fate_grand_automata.scripts.entrypoints.CommandCodeImageMaker
import io.github.fate_grand_automata.util.StorageProvider
import io.github.fate_grand_automata.util.dayNightThemed
import io.github.fate_grand_automata.util.showOverlayDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.math.roundToInt

// Reference images are matched at their own native pixel dimensions with no runtime resizing
// (DroidCvPattern decodes the PNG as-is) -- see CommandCodeImageMaker's own doc comment for why
// this crops straight from a live, already-matching-space screenshot rather than an externally
// re-derived one. 53px was measured directly against such an image on a real device: 80px (an
// earlier size, from before that live-capture approach existed) still pulled in background
// around the badge.
private const val CropSize = 53

// *, ?, \, |, / are special characters in Regex and need to be escaped using \
private val InvalidFileNameChars = """<>"\|:\*\?\\\/"""
private val FileNameRegex = Regex("""[^.\s$InvalidFileNameChars][^$InvalidFileNameChars]*""")

/**
 * Shows the just-captured screenshot ([CommandCodeImageMaker]) with a draggable [CropSize]-px
 * box, and lets the user save as many differently-named crops out of it as they like (one
 * screenshot may show several badges at once) before closing.
 *
 * Save sits in the dialog's own button bar (as the neutral button, immediately left of Done --
 * AlertDialog lays out [negative, neutral, positive] left to right and this dialog has no
 * negative button) rather than inside the scrollable content: a captured screenshot is wide
 * (matching space is 720-1280px tall), and on a real device the crop box + name field could
 * already fill the dialog's visible height, leaving an in-content Save button scrolled out of
 * sight with no visible cue that it needed scrolling to reach.
 */
suspend fun showCommandCodeImageNamer(context: Context, storageProvider: StorageProvider) =
    withContext(Dispatchers.Main) {
        val themedContext = context.dayNightThemed()

        val capturePath = CommandCodeImageMaker.getCaptureImgPath(storageProvider.supportImageTempDir)
        // inScaled = false: BitmapFactory otherwise scales the decoded bitmap to the device's
        // current display density when the file carries no density metadata of its own, which
        // would silently reintroduce the exact kind of resize distortion this whole live-capture
        // approach exists to avoid.
        val bitmap = BitmapFactory.decodeFile(
            capturePath.absolutePath,
            BitmapFactory.Options().apply { inScaled = false }
        )

        val cropView = CropOverlayView(themedContext, CropSize)
        val statusText = TextView(themedContext)
        val nameField = EditText(themedContext).apply {
            hint = context.getString(R.string.command_code_namer_name_hint)
        }

        if (bitmap != null) {
            cropView.setBitmap(bitmap)
        } else {
            statusText.text = context.getString(R.string.command_code_namer_capture_missing)
            nameField.isEnabled = false
        }

        fun performSave() {
            val name = nameField.text.toString().trim()

            when {
                name.isBlank() ->
                    statusText.text = context.getString(R.string.command_code_namer_blank_name)

                !FileNameRegex.matches(name) ->
                    statusText.text = context.getString(
                        R.string.support_img_namer_invalid_message,
                        "<, >, \", |, :, *, ?, \\, /"
                    )

                else -> {
                    val rect = cropView.currentCropRect()
                    val cropped = Bitmap.createBitmap(bitmap!!, rect.left, rect.top, rect.width(), rect.height())

                    try {
                        storageProvider.writeSupportImage(SupportImageKind.CommandCode, "$name.png").use { out ->
                            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }

                        statusText.text = context.getString(R.string.command_code_namer_saved, name)
                        nameField.text.clear()
                    } catch (e: Exception) {
                        Timber.e(e, "CommandCodeImageNamer: failed to save '$name.png'")
                        statusText.text = context.getString(R.string.support_img_namer_file_rename_failed, name)
                    }
                }
            }
        }

        val root = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).roundToInt()
            setPadding(pad, pad, pad, pad)

            addView(cropView)
            addView(nameField)
            addView(statusText)
        }

        val scrollContent = ScrollView(themedContext).apply {
            addView(root)
        }

        suspendCancellableCoroutine { coroutine ->
            val dialog = showOverlayDialog(context) {
                setTitle(context.getString(R.string.command_code_namer_title))
                    .setView(scrollContent)
                    .setPositiveButton(context.getString(R.string.support_img_namer_done)) { d, _ ->
                        d.dismiss()
                    }
                    // Passing null here (rather than a listener) is deliberate: AlertDialog's
                    // built-in button listeners always dismiss the dialog once they return, with
                    // no way to opt out from inside the listener itself. Save must NOT dismiss
                    // (there can be several badges to crop from one capture), so its real
                    // behavior is wired below via getButton(), which replaces the default
                    // listener with a plain View.OnClickListener that dismisses only when told to.
                    .setNeutralButton(context.getString(R.string.command_code_namer_save), null)
                    .setOnDismissListener {
                        coroutine.resume(Unit)
                    }
            }

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.apply {
                isEnabled = bitmap != null
                setOnClickListener { performSave() }
            }
        }
    }
