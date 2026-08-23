package com.djangofiles.djangofiles.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Shows the soft keyboard for this dialog window.
 *
 * Copied from androidx.preference PreferenceDialogFragmentCompat.requestInputMethod()
 * which is how EditTextPreference dialogs shows the keyboard when a dialog is shown.
 *
 * https://github.com/androidx/androidx/blob/androidx-main/preference/preference/src/main/java/androidx/preference/PreferenceDialogFragmentCompat.java
 *
 * AI NOTE: Call AFTER create() and BEFORE show() (like the library calls requestInputMethod
 * in onCreateDialog). The focused editor and window flags must be in place before the
 * dialog window gains focus or the keyboard will not show reliably.
 */
fun Dialog.showKeyboard() {
    val window: Window = window ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        slideAboveIme()
        // Same as androidx.preference Api30Impl.showIme(window)
        window.decorView.windowInsetsController?.show(WindowInsets.Type.ime())
    } else {
        // AI NOTE: Below R, WindowInsetsCompat.Type.ime() carries no data, so fall back to the
        // legacy system pan behavior there.
        // NOTE: SOFT_INPUT_ADJUST_PAN prevents shrinking the dialog
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )

        // TODO: Validate code below here - added to show keyboard in landscape in API <30
        // AI NOTE: Port of androidx.preference EditTextPreferenceDialogFragmentCompat
        // scheduleShowSoftInputInner(): below Android R, imm.showSoftInput() is
        // silently refused while the dialog window has not gained focus yet
        // (async gap between show() and focus arriving), so retry every
        // SHOW_RETRY_DELAY_MS until the system accepts the request or the
        // SHOW_REQUEST_TIMEOUT_MS budget runs out - same values as the library.
        val startMs = SystemClock.uptimeMillis()

        fun tryShow() {
            val editor = window.currentFocus ?: window.decorView.findFocus()
            if (editor != null) {
                val imm = editor.context.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                if (imm.showSoftInput(editor, 0)) {
                    return
                }
            }
            if (SystemClock.uptimeMillis() - startMs < SHOW_REQUEST_TIMEOUT_MS) {
                window.decorView.postDelayed({ tryShow() }, SHOW_RETRY_DELAY_MS)
            }
        }

        tryShow()
    }
}

// AI NOTE: The dialog keeps its NATURAL SIZE and is TRANSLATED upward into the empty space
// between its top and the top of the screen, until either its bottom edge clears the
// keyboard or its top reaches just below the status bar. This fills the gap above instead
// of shrinking (SOFT_INPUT_ADJUST_RESIZE squashes the whole AlertDialog window frame into
// the leftover strip and makes it unreadable) and instead of panning
// (SOFT_INPUT_ADJUST_PAN only moves the window until the FOCUSED editor clears the top of
// the keyboard - ViewRootImpl scrollY = focusRect.top - visibleTop - which leaves dead
// space above the dialog while the bottom buttons stay covered).
//
// Mechanics: a Dialog has its own Window with its own softInputMode; the activity manifest
// setting never applies to it. ADJUST_NOTHING disables both built-in behaviors so nothing
// fights this manual translation, and setDecorFitsSystemWindows(false) lets the raw ime()
// insets through to the listener. Per AOSP InsetsState.processSource(), ime() insets are
// calculated relative to THIS window's frame, so ime.bottom on the dialog = exactly how
// many pixels of it the keyboard covers.
private fun Dialog.slideAboveIme() {
    val window = window ?: return
    val decor = window.decorView
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
    )
    ViewCompat.setOnApplyWindowInsetsListener(decor) { v, insets ->
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val barsTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        if (!imeVisible || imeBottom <= 0) {
            if (v.translationY != 0f) v.translationY = 0f
            return@setOnApplyWindowInsetsListener insets
        }
        // Post: getLocationOnScreen() can be stale mid-layout during inset dispatch.
        // translationY is subtracted back out so repeated callbacks stay anchored to the
        // window's untranslated position instead of drifting upward every callback.
        v.post {
            val location = IntArray(2)
            v.getLocationOnScreen(location)
            val baseTop = location[1] - v.translationY.toInt()
            // Max distance the dialog can move up: down to just below the status bar.
            val maxUp = (baseTop - barsTop).coerceAtLeast(0)
            val shift = imeBottom.coerceAtMost(maxUp)
            if (shift > 0) {
                v.translationY = -shift.toFloat()
            } else if (v.translationY != 0f) {
                v.translationY = 0f
            }
        }
        insets
    }
    // Re-evaluate when the dialog's own layout changes (e.g. the multiline feedback
    // EditText grows between minLines and maxLines while typing).
    decor.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        ViewCompat.requestApplyInsets(view)
    }
}

// Same budget as androidx.preference (SHOW_REQUEST_TIMEOUT = 1000).
private const val SHOW_REQUEST_TIMEOUT_MS = 1000L

// Same retry interval as androidx.preference (postDelayed(..., 50)).
private const val SHOW_RETRY_DELAY_MS = 50L
