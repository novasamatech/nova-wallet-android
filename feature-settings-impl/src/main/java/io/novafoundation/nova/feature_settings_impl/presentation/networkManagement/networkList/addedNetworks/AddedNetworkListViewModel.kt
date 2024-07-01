package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkListRvItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddedNetworkListViewModel(
    private val networkManagementInteractor: NetworkManagementInteractor,
    private val networkListAdapterItemFactory: NetworkListAdapterItemFactory,
    private val appLinksProvider: AppLinksProvider,
    router: SettingsRouter
) : NetworkListViewModel(router), Browserable {

    private val networks = networkManagementInteractor.addedNetworksFlow()
        .shareInBackground()

    override val networkList: Flow<List<NetworkListRvItem>> = networks.mapList {
        networkListAdapterItemFactory.getNetworkItem(it)
    }

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val showBanner = networkManagementInteractor.shouldShowBanner()
        .shareInBackground()

    fun closeBannerClicked() {
        launch {
            networkManagementInteractor.hideBanner()
        }
    }

    fun bannerWikiClicked() {
        openBrowserEvent.value = appLinksProvider.integrateNetwork.event()
    }

    fun addNetworkClicked() {
        showError("Not implemented")
    }
}
