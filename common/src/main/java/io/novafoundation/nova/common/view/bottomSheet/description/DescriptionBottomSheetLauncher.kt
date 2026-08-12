package io.novafoundation.nova.common.view.bottomSheet.description

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event

interface DescriptionBottomSheetLauncher {

    val showDescriptionEvent: LiveData<Event<DescriptionModel>>

    fun launchDescriptionBottomSheet(title: String, description: String)
}

fun DescriptionBottomSheetLauncher.launchNetworkFeeDescription(resourceManager: ResourceManager) {
    launchDescriptionBottomSheet(
        title = resourceManager.getString(R.string.network_fee),
        description = resourceManager.getString(R.string.swap_network_fee_description)
    )
}

class RealDescriptionBottomSheetLauncher : DescriptionBottomSheetLauncher {

    override val showDescriptionEvent = MutableLiveData<Event<DescriptionModel>>()

    override fun launchDescriptionBottomSheet(title: String, description: String) {
        showDescriptionEvent.value = DescriptionModel(title, description).event()
    }
}

fun BaseFragment<*, *>.observeDescription(launcher: DescriptionBottomSheetLauncher) {
    launcher.showDescriptionEvent.observeEvent { event ->
        DescriptionBottomSheet(requireContext(), event.title, event.description).show()
    }
}
