package io.novafoundation.nova.common.view.bottomSheet.description

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event

interface DescriptionBottomSheetLauncher {

    val showDescriptionEvent: LiveData<Event<DescriptionModel>>

    fun launchDescriptionBottomSheet(titleRes: Int, descriptionRes: Int)
}

class RealDescriptionBottomSheetLauncher : DescriptionBottomSheetLauncher {

    override val showDescriptionEvent = MutableLiveData<Event<DescriptionModel>>()

    override fun launchDescriptionBottomSheet(titleRes: Int, descriptionRes: Int) {
        showDescriptionEvent.value = DescriptionModel(titleRes, descriptionRes).event()
    }
}

fun BaseFragment<*>.observeDescription(launcher: DescriptionBottomSheetLauncher) {
    launcher.showDescriptionEvent.observeEvent { event ->
        val dialog = DescriptionBottomSheet(
            context = requireContext(),
            titleRes = event.titleRes,
            descriptionRes = event.descriptionRes
        )

        dialog.show()
    }
}
