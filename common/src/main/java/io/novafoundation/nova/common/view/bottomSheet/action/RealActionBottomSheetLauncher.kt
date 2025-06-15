package io.novafoundation.nova.common.view.bottomSheet.action

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event

class RealActionBottomSheetLauncherFactory : ActionBottomSheetLauncherFactory {

    override fun create(): ActionBottomSheetLauncher {
        return RealActionBottomSheetLauncher()
    }
}

class RealActionBottomSheetLauncher : ActionBottomSheetLauncher {

    override val showActionEvent = MutableLiveData<Event<ActionBottomSheetPayload>>()

    override fun launchBottomSheet(
        @DrawableRes imageRes: Int,
        title: CharSequence,
        subtitle: CharSequence,
        actionButtonPreferences: ButtonPreferences,
        neutralButtonPreferences: ButtonPreferences?,
        checkBoxPreferences: CheckBoxPreferences?
    ) {
        showActionEvent.value = ActionBottomSheetPayload(
            imageRes = imageRes,
            title = title,
            subtitle = subtitle,
            actionButtonPreferences = actionButtonPreferences,
            neutralButtonPreferences = neutralButtonPreferences,
            checkBoxPreferences = checkBoxPreferences
        ).event()
    }
}
