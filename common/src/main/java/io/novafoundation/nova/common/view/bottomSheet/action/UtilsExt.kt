package io.novafoundation.nova.common.view.bottomSheet.action

import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import io.novafoundation.nova.common.databinding.BottomSheetActionBinding
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.common.view.PrimaryButton

fun BottomSheetActionBinding.setupView(
    payload: ActionBottomSheetPayload,
    onPositiveButtonClicked: (() -> Unit)?,
    onNeutralButtonClicked: (() -> Unit)?
) {
    val iconView: ImageView = actionBottomSheetImage
    val titleView: TextView = actionBottomSheetTitle
    val subtitleView: TextView = actionBottomSheetSubtitle
    val neutralButton: PrimaryButton = actionBottomSheetNeutralBtn
    val actionButton: PrimaryButton = actionBottomSheetPositiveBtn
    val alert: AlertView = actionBottomSheetAlert
    val checkBox: CheckBox = actionBottomSheetCheckBox

    iconView.setImageResource(payload.imageRes)
    titleView.text = payload.title
    subtitleView.text = payload.subtitle

    actionButton.apply {
        text = payload.actionButtonPreferences.text
        setAppearance(payload.actionButtonPreferences.style)
        setOnClickListener {
            payload.actionButtonPreferences.onClick?.invoke()
            onPositiveButtonClicked?.invoke()
        }
    }

    payload.neutralButtonPreferences?.let { preferences ->
        neutralButton.apply {
            text = preferences.text
            setAppearance(preferences.style)
            setOnClickListener {
                preferences.onClick?.invoke()
                onNeutralButtonClicked?.invoke()
            }
        }
    } ?: neutralButton.makeGone()

    payload.alertModel?.let {
        alert.apply {
            makeVisible()
            setStyle(it.style)
            setMessage(it.message)
        }
    } ?: alert.makeGone()

    payload.checkBoxPreferences?.let { preferences ->
        checkBox.text = preferences.text
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            preferences.onCheckChanged?.invoke(isChecked)
        }
    } ?: checkBox.makeGone()
}
