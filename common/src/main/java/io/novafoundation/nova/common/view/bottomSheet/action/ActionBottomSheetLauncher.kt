package io.novafoundation.nova.common.view.bottomSheet.action

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.base.BaseScreenMixin
import io.novafoundation.nova.common.utils.Event

interface ActionBottomSheetLauncherFactory {

    fun create(): ActionBottomSheetLauncher
}

interface ActionBottomSheetLauncher {

    val showActionEvent: LiveData<Event<ActionBottomSheetPayload>>

    fun launchBottomSheet(
        @DrawableRes imageRes: Int,
        title: CharSequence,
        subtitle: CharSequence,
        actionButtonPreferences: ButtonPreferences,
        neutralButtonPreferences: ButtonPreferences? = null,
        checkBoxPreferences: CheckBoxPreferences? = null
    )
}

fun BaseScreenMixin<*>.observeActionBottomSheet(launcher: ActionBottomSheetLauncher) {
    launcher.showActionEvent.observeEvent { payload ->
        val dialog = ActionBottomSheet(
            context = providedContext,
            payload = payload
        )

        dialog.show()
    }
}
