package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.PreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkListRvItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class PreConfiguredNetworksViewModel(
    private val preConfiguredNetworksInteractor: PreConfiguredNetworksInteractor,
    private val networkListAdapterItemFactory: NetworkListAdapterItemFactory,
    private val router: SettingsRouter
) : BaseViewModel() {

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    private val allPreConfiguredNetworks = flowOf { preConfiguredNetworksInteractor.getPreConfiguredNetworks() }
        .shareInBackground()

    private val networks = combine(
        allPreConfiguredNetworks,
        searchQuery
    ) { networks, query ->
        preConfiguredNetworksInteractor.filterNetworks(query, networks)
    }

    val networkList: Flow<ExtendedLoadingState<List<NetworkListRvItem>>> = networks.mapList {
        networkListAdapterItemFactory.getNetworkItem(it)
    }

    fun backClicked() {

    }

    fun networkClicked(chainId: String) {

    }

    fun addNetworkClicked() {

    }
}
