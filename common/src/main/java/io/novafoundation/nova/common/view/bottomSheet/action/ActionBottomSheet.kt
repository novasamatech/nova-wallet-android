package io.novafoundation.nova.common.view.bottomSheet.action

import android.content.Context
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet

class ActionBottomSheet(
    context: Context,
    payload: Payload
) : BaseBottomSheet(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    class Payload(
        @DrawableRes val imageRes: Int,
        val title: CharSequence,
        val subtitle: CharSequence,
        val actionButtonPreferences: ButtonPreferences,
        val neutralButtonPreferences: ButtonPreferences? = null,
        val checkBoxPreferences: CheckBoxPreferences? = null
    )

    class ButtonPreferences(
        val text: CharSequence,
        val style: PrimaryButton.Appearance,
        val onClick: (() -> Unit)? = null
    ) {
        companion object
    }

    class CheckBoxPreferences(
        val text: CharSequence,
        val onCheckChanged: ((Boolean) -> Unit)? = null
    ) {
        companion object
    }

    private val iconView: ImageView
        get() = actionBottomSheetImage

    private val titleView: TextView
        get() = actionBottomSheetTitle

    private val subtitleView: TextView
        get() = actionBottomSheetSubtitle

    private val neutralButton: PrimaryButton
        get() = actionBottomSheetNeutralBtn

    private val actionButton: PrimaryButton
        get() = actionBottomSheetPositiveBtn

    private val checkBox: CheckBox
        get() = actionBottomSheetCheckBox

    init {
        setContentView(R.layout.bottom_sheet_action)

        iconView.setImageResource(payload.imageRes)
        titleView.text = payload.title
        subtitleView.text = payload.subtitle

        actionButton.apply {
            text = payload.actionButtonPreferences.text
            setAppearance(payload.actionButtonPreferences.style)
            setOnClickListener {
                payload.actionButtonPreferences.onClick?.invoke()
                dismiss()
            }
        }

        payload.neutralButtonPreferences?.let { preferences ->
            neutralButton.apply {
                text = preferences.text
                setAppearance(preferences.style)
                setOnClickListener {
                    preferences.onClick?.invoke()
                    dismiss()
                }
            }
        } ?: neutralButton.makeGone()

        payload.checkBoxPreferences?.let { preferences ->
            checkBox.text = preferences.text
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                preferences.onCheckChanged?.invoke(isChecked)
            }
        } ?: checkBox.makeGone()
    }
}

fun ActionBottomSheet.ButtonPreferences.Companion.primary(text: CharSequence, onClick: (() -> Unit)? = null) =
    ActionBottomSheet.ButtonPreferences(text, PrimaryButton.Appearance.PRIMARY, onClick)

fun ActionBottomSheet.ButtonPreferences.Companion.secondary(text: CharSequence, onClick: (() -> Unit)? = null) =
    ActionBottomSheet.ButtonPreferences(text, PrimaryButton.Appearance.SECONDARY, onClick)

fun ActionBottomSheet.ButtonPreferences.Companion.negative(text: CharSequence, onClick: (() -> Unit)? = null) =
    ActionBottomSheet.ButtonPreferences(text, PrimaryButton.Appearance.PRIMARY_NEGATIVE, onClick)
