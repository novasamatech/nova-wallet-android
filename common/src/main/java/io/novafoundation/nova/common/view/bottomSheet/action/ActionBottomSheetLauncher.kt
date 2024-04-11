package io.novafoundation.nova.common.view.bottomSheet.action

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event

interface ActionBottomSheetLauncher {

    val showActionEvent: LiveData<Event<ActionBottomSheet.Payload>>

    fun launchBottomSheet(
        @DrawableRes imageRes: Int,
        title: CharSequence,
        subtitle: CharSequence,
        actionButtonPreferences: ActionBottomSheet.ButtonPreferences,
        neutralButtonPreferences: ActionBottomSheet.ButtonPreferences? = null
    )
}

class RealActionBottomSheetLauncher : ActionBottomSheetLauncher {

    override val showActionEvent = MutableLiveData<Event<ActionBottomSheet.Payload>>()

    override fun launchBottomSheet(
        @DrawableRes imageRes: Int,
        title: CharSequence,
        subtitle: CharSequence,
        actionButtonPreferences: ActionBottomSheet.ButtonPreferences,
        neutralButtonPreferences: ActionBottomSheet.ButtonPreferences?
    ) {
        showActionEvent.value = ActionBottomSheet.Payload(
            imageRes = imageRes,
            title = title,
            subtitle = subtitle,
            actionButtonPreferences = actionButtonPreferences,
            neutralButtonPreferences = neutralButtonPreferences
        ).event()
    }
}

fun BaseFragment<*>.observeActionBottomSheet(launcher: ActionBottomSheetLauncher) {
    launcher.showActionEvent.observeEvent { payload ->
        val dialog = ActionBottomSheet(
            context = requireContext(),
            payload = payload
        )

        dialog.show()
    }
}
