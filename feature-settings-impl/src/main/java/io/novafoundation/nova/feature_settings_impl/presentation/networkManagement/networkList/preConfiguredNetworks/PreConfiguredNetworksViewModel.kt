package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.PreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

typealias LoadingNetworks = ExtendedLoadingState<List<LightChain>>

class PreConfiguredNetworksViewModel(
    private val preConfiguredNetworksInteractor: PreConfiguredNetworksInteractor,
    private val networkListAdapterItemFactory: NetworkListAdapterItemFactory,
    private val router: SettingsRouter,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry
) : BaseViewModel(), Retriable {

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    private val allPreConfiguredNetworksFlow = MutableStateFlow<LoadingNetworks>(ExtendedLoadingState.Loading)

    private val networks = combine(
        allPreConfiguredNetworksFlow,
        searchQuery,
        chainRegistry.chainsById
    ) { preConfiguredNetworks, query, currentChains ->
        preConfiguredNetworks.map {
            val filteredNetworks = preConfiguredNetworksInteractor.excludeChains(it, currentChains.keys)
            preConfiguredNetworksInteractor.searchNetworks(query, filteredNetworks)
        }
    }

    val networkList = networks.mapLoading { networks ->
        networks.map { networkListAdapterItemFactory.getNetworkItem(it) }
    }

    override val retryEvent: MutableLiveData<Event<RetryPayload>> = MutableLiveData()

    init {
        fetchPreConfiguredNetworks()
    }

    fun backClicked() {
        router.back()
    }

    fun networkClicked(chainId: String) {
        showMessage("Not implemented yet")
    }

    fun addNetworkClicked() {
        showMessage("Not implemented yet")
    }

    private fun fetchPreConfiguredNetworks() {
        launch {
            allPreConfiguredNetworksFlow.value = ExtendedLoadingState.Loading

            preConfiguredNetworksInteractor.getPreConfiguredNetworks()
                .onSuccess {
                    allPreConfiguredNetworksFlow.value = ExtendedLoadingState.Loaded(it)
                }.onFailure {
                    launchRetryDialog()
                }
        }
    }

    private fun launchRetryDialog() {
        retryEvent.value = Event(
            RetryPayload(
                title = resourceManager.getString(R.string.common_error_general_title),
                message = resourceManager.getString(R.string.common_retry_message),
                onRetry = { fetchPreConfiguredNetworks() },
                onCancel = { router.back() }
            )
        )
    }
}
