package io.novafoundation.nova.common.view.bottomSheet.action

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.PrimaryButton

class ActionBottomSheetPayload(
    @DrawableRes val imageRes: Int,
    val title: CharSequence,
    val subtitle: CharSequence,
    val actionButtonPreferences: ButtonPreferences,
    val neutralButtonPreferences: ButtonPreferences? = null,
    val alertModel: AlertModel? = null,
    val checkBoxPreferences: CheckBoxPreferences? = null
)

class CheckBoxPreferences(
    val text: CharSequence,
    val onCheckChanged: ((Boolean) -> Unit)? = null
) {
    companion object
}

class ButtonPreferences(
    val text: CharSequence,
    val style: PrimaryButton.Appearance,
    val onClick: (() -> Unit)? = null
) {
    companion object
}

fun ButtonPreferences.Companion.primary(text: CharSequence, onClick: (() -> Unit)? = null) =
    ButtonPreferences(text, PrimaryButton.Appearance.PRIMARY, onClick)

fun ButtonPreferences.Companion.secondary(text: CharSequence, onClick: (() -> Unit)? = null) =
    ButtonPreferences(text, PrimaryButton.Appearance.SECONDARY, onClick)

fun ButtonPreferences.Companion.negative(text: CharSequence, onClick: (() -> Unit)? = null) =
    ButtonPreferences(text, PrimaryButton.Appearance.PRIMARY_NEGATIVE, onClick)
