package com.example.ccvpayment.helper

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.ccvpayment.R

/**
 * Helper class for displaying popup messages in the center of the screen.
 *
 * This class provides methods to show success, error, info, and warning messages
 * as centered popup dialogs instead of Toast messages. The popups automatically
 * dismiss after a specified duration.
 *
 * Usage:
 * ```kotlin
 * PopupMessageHelper.showSuccess(context, "Operation completed successfully")
 * PopupMessageHelper.showError(context, "An error occurred")
 * PopupMessageHelper.showInfo(context, "Information message")
 * PopupMessageHelper.showWarning(context, "Warning message")
 * ```
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
object PopupMessageHelper {

    /**
     * Default duration for popup display in milliseconds.
     */
    private const val DEFAULT_DURATION = 2000L

    /**
     * Short duration for popup display in milliseconds.
     */
    private const val SHORT_DURATION = 1500L

    /**
     * Long duration for popup display in milliseconds.
     */
    private const val LONG_DURATION = 3500L

    /**
     * Enum class representing the type of popup message.
     *
     * @property iconResId Resource ID for the icon
     * @property colorResId Resource ID for the background color
     */
    enum class MessageType(val iconResId: Int, val colorResId: Int) {
        SUCCESS(R.drawable.ic_payment, R.color.payment_success),
        ERROR(R.drawable.ic_refund, R.color.payment_failed),
        INFO(R.drawable.ic_info, R.color.primary),
        WARNING(R.drawable.ic_warning, R.color.status_yellow)
    }

    /**
     * Shows a success popup message in the center of the screen.
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     * @param duration Duration in milliseconds before auto-dismiss (default: 2000ms)
     */
    @JvmStatic
    @JvmOverloads
    fun showSuccess(context: Context, message: String, duration: Long = DEFAULT_DURATION) {
        showPopup(context, message, MessageType.SUCCESS, duration)
    }

    /**
     * Shows an error popup message in the center of the screen.
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     * @param duration Duration in milliseconds before auto-dismiss (default: 2000ms)
     */
    @JvmStatic
    @JvmOverloads
    fun showError(context: Context, message: String, duration: Long = DEFAULT_DURATION) {
        showPopup(context, message, MessageType.ERROR, duration)
    }

    /**
     * Shows an info popup message in the center of the screen.
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     * @param duration Duration in milliseconds before auto-dismiss (default: 2000ms)
     */
    @JvmStatic
    @JvmOverloads
    fun showInfo(context: Context, message: String, duration: Long = DEFAULT_DURATION) {
        showPopup(context, message, MessageType.INFO, duration)
    }

    /**
     * Shows a warning popup message in the center of the screen.
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     * @param duration Duration in milliseconds before auto-dismiss (default: 2000ms)
     */
    @JvmStatic
    @JvmOverloads
    fun showWarning(context: Context, message: String, duration: Long = DEFAULT_DURATION) {
        showPopup(context, message, MessageType.WARNING, duration)
    }

    /**
     * Shows a popup message with the specified type.
     *
     * Creates a centered dialog with an icon and message that automatically
     * dismisses after the specified duration.
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     * @param type The type of message (SUCCESS, ERROR, INFO, WARNING)
     * @param duration Duration in milliseconds before auto-dismiss
     */
    private fun showPopup(context: Context, message: String, type: MessageType, duration: Long) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.popup_message, null)
        dialog.setContentView(view)

        // Set up the icon
        val iconView = view.findViewById<ImageView>(R.id.popupIcon)
        iconView.setImageResource(type.iconResId)
        iconView.setColorFilter(context.getColor(type.colorResId))

        // Set up the message
        val messageView = view.findViewById<TextView>(R.id.popupMessage)
        messageView.text = message

        // Configure dialog window
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            // Dim the background
            setDimAmount(0.5f)
        }

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        dialog.show()

        // Auto dismiss after duration
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, duration)
    }

    /**
     * Shows a simple message popup (default: INFO type).
     *
     * @param context The context to use for creating the dialog
     * @param message The message to display
     */
    @JvmStatic
    fun show(context: Context, message: String) {
        showInfo(context, message)
    }
}
