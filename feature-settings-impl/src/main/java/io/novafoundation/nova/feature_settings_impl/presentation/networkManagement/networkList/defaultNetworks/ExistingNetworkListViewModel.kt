package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListRvItem
import kotlinx.coroutines.flow.Flow

class ExistingNetworkListViewModel(
    private val networkManagementInteractor: NetworkManagementInteractor,
    private val networkListAdapterItemFactory: NetworkListAdapterItemFactory,
    router: SettingsRouter,
    resourceManager: ResourceManager,
) : NetworkListViewModel() {

    private val networks = networkManagementInteractor.defaultNetworksFlow()
        .shareInBackground()

    override val networkList: Flow<List<NetworkListRvItem>> = networks.mapList {
        networkListAdapterItemFactory.getNetworkItem(it)
    }
}
