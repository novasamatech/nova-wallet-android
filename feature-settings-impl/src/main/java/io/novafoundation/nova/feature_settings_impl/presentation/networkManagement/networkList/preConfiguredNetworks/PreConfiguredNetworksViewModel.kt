package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.utils.progress.startProgress
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.PreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.runtime.ext.evmChainIdOrNull
import io.novafoundation.nova.runtime.ext.networkType
import io.novafoundation.nova.runtime.ext.normalizedUrl
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

typealias LoadingNetworks = ExtendedLoadingState<List<LightChain>>

class PreConfiguredNetworksViewModel(
    private val interactor: PreConfiguredNetworksInteractor,
    private val networkListAdapterItemFactory: NetworkListAdapterItemFactory,
    private val router: SettingsRouter,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val progressDialogMixinFactory: ProgressDialogMixinFactory,
    private val coinGeckoLinkParser: CoinGeckoLinkParser
) : BaseViewModel(), Retriable {

    val progressDialogMixin = progressDialogMixinFactory.create()

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    private val allPreConfiguredNetworksFlow = MutableStateFlow<LoadingNetworks>(ExtendedLoadingState.Loading)

    private val networks = combine(
        allPreConfiguredNetworksFlow,
        searchQuery,
        chainRegistry.chainsById
    ) { preConfiguredNetworks, query, currentChains ->
        preConfiguredNetworks.map {
            val filteredNetworks = interactor.excludeChains(it, currentChains.keys)
            interactor.searchNetworks(query, filteredNetworks)
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
        launch {
            progressDialogMixin.startProgress(R.string.loading_network_info) {
                interactor.getPreConfiguredNetwork(chainId)
                    .onSuccess {
                        openAddChainScreen(it)
                    }
                    .onFailure { showError(resourceManager.getString(R.string.common_something_went_wrong_title)) }
            }
        }
    }

    private fun openAddChainScreen(chain: Chain) {
        val (networkType, chainId) = when (chain.networkType()) {
            NetworkType.SUBSTRATE -> AddNetworkPayload.Mode.Add.NetworkType.SUBSTRATE to null
            NetworkType.EVM -> AddNetworkPayload.Mode.Add.NetworkType.EVM to chain.evmChainIdOrNull()
        }

        val node = chain.nodes.nodes.firstOrNull()
        val asset = chain.assets.firstOrNull()
        val explorer = chain.explorers.firstOrNull()

        val payload = AddNetworkPayload.Mode.Add(
            networkType = networkType,
            AddNetworkPayload.Mode.Add.NetworkData(
                iconUrl = chain.icon,
                isTestNet = chain.isTestNet,
                rpcNodeUrl = node?.unformattedUrl,
                networkName = chain.name,
                tokenName = asset?.symbol?.value,
                evmChainId = chainId,
                blockExplorerUrl = explorer?.normalizedUrl(),
                coingeckoLink = asset?.priceId?.let { coinGeckoLinkParser.format(it) }
            )
        )

        router.openCreateNetworkFlow(payload)
    }

    fun addNetworkClicked() {
        router.openCreateNetworkFlow()
    }

    private fun fetchPreConfiguredNetworks() {
        launch {
            allPreConfiguredNetworksFlow.value = ExtendedLoadingState.Loading

            interactor.getPreConfiguredNetworks()
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
