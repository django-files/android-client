package com.djangofiles.djangofiles.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

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
        // Same as androidx.preference Api30Impl.showIme(window)
        window.decorView.windowInsetsController?.show(WindowInsets.Type.ime())
    } else {
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

// Same budget as androidx.preference (SHOW_REQUEST_TIMEOUT = 1000).
private const val SHOW_REQUEST_TIMEOUT_MS = 1000L

// Same retry interval as androidx.preference (postDelayed(..., 50)).
private const val SHOW_RETRY_DELAY_MS = 50L
