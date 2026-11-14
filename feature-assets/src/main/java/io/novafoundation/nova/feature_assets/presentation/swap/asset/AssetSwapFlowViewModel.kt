package io.novafoundation.nova.feature_assets.presentation.swap.asset

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.firstIfLoaded
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.account.SelectedMetaAccountChangeObserver
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.swap.SwapNetworkSelectionRepository
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSelectionData
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSelectionInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class AssetSwapFlowViewModel(
    private val interactor: SwapTokenSelectionInteractor,
    private val formatter: SwapTokenSelectionFormatter,
    private val networkSelectionRepository: SwapNetworkSelectionRepository,
    private val router: AssetsRouter,
    private val resourceManager: ResourceManager,
    private val accountRepository: AccountRepository,
    private val selectedMetaAccountChangeObserver: SelectedMetaAccountChangeObserver,
    private val communicator: SwapTokenSelectionCommunicator,
    private val request: SwapTokenSelectionCommunicator.Request
) : BaseViewModel() {

    val query = MutableStateFlow("")

    private val selectedNetworks = MutableStateFlow<Set<ChainId>>(emptySet())

    private val retries = MutableStateFlow(0)

    private val selecting = MutableStateFlow(false)

    private var selectionJob: Job? = null

    private val data = retries.flatMapLatest {
        interactor.observe(request.metaAccountId, request.oppositeAsset?.fullChainAssetId, request.selectingPay, viewModelScope)
            .withSafeLoading()
            .onError { error ->
                if (error is CancellationException) throw error
                error.printStackTrace()
            }
    }.shareInBackground()

    private val networkSelectionFlow = combine(
        networkSelectionRepository.savedNetworks(),
        selectedNetworks
    ) { savedNetworkIds, selectedNetworkIds ->
        NetworkSelection(savedNetworkIds, selectedNetworkIds)
    }

    internal val screen = combine(
        data,
        query,
        networkSelectionFlow,
        selecting
    ) { state, query, networkSelection, selecting ->
        val title = resourceManager.getString(if (request.selectingPay) R.string.swap_select_pay_title else R.string.swap_select_receive_title)
        when (state) {
            ExtendedLoadingState.Loading -> SwapTokenSelectionModel(title, loading = true)
            is ExtendedLoadingState.Error -> SwapTokenSelectionModel(title, failed = true)
            is ExtendedLoadingState.Loaded -> loadedScreen(title, state.data, query, networkSelection, selecting)
        }
    }.shareInBackground()

    init {
        selectedMetaAccountChangeObserver.observeOnce(
            initialMetaAccountId = request.metaAccountId,
            scope = viewModelScope
        ) { close() }
    }

    fun retry() {
        retries.value += 1
    }

    fun close() {
        selectionJob?.cancel()
        router.back()
    }

    internal fun chipClicked(chip: SwapNetworkChip) {
        when (chip) {
            is SwapNetworkChip.All -> selectedNetworks.value = emptySet()
            is SwapNetworkChip.Network -> {
                val id = chip.network.chainId
                selectedNetworks.value = if (id in selectedNetworks.value) selectedNetworks.value - id else selectedNetworks.value + id
            }
            is SwapNetworkChip.More -> Unit // The Fragment presents the network selection sheet.
        }
    }

    fun saveNetworks(networks: Set<ChainId>) {
        networkSelectionRepository.saveNetworks(networks)
        selectedNetworks.value = networks
    }

    internal fun rowClicked(row: SwapTokenRow) {
        if (!selecting.value) select(row.id)
    }

    private fun loadedScreen(
        title: String,
        data: SwapTokenSelectionData,
        query: String,
        networkSelection: NetworkSelection,
        selecting: Boolean
    ): SwapTokenSelectionModel {
        val chips = interactor.networkChips(data.chains, networkSelection.savedNetworkIds)
        val availableNetworks = data.chains.map { it.id }.toSet()
        val selected = networkSelection.selectedNetworkIds.intersect(availableNetworks)
        val moreCount = data.chains.size - chips.size
        val networks = (interactor.networkChips(data.chains, emptySet()) + data.chains.sortedWith(Chain.defaultComparator())).distinctBy { it.id }
        return SwapTokenSelectionModel(
            title = title,
            items = formatter.tokens(interactor.groups(data, query, selected), data.oppositeSymbol, request.selectedAsset?.fullChainAssetId),
            networks = networks.map { formatter.network(it, it.id in networkSelection.savedNetworkIds) },
            chips = buildList {
                add(SwapNetworkChip.All(resourceManager.getString(R.string.common_all), selected.isEmpty()))
                addAll(chips.map { SwapNetworkChip.Network(formatter.network(it, it.id in selected)) })
                add(
                    SwapNetworkChip.More(
                        if (moreCount > 0) {
                            resourceManager.getString(R.string.swap_select_more_networks, moreCount)
                        } else {
                            resourceManager.getString(R.string.select_network_title)
                        }
                    )
                )
            },
            selecting = selecting
        )
    }

    private fun select(id: FullChainAssetId) {
        selectionJob = launch {
            selecting.value = true
            try {
                val loaded = data.firstIfLoaded() ?: return@launch
                val asset = interactor.asset(loaded, id) ?: return@launch
                val clearOpposite = interactor.shouldClearOpposite(
                    id,
                    request.oppositeAsset?.fullChainAssetId,
                    request.selectingPay,
                    request.metaAccountId,
                    viewModelScope
                )
                if (accountRepository.getSelectedMetaAccount().id != request.metaAccountId) return@launch
                communicator.respond(
                    SwapTokenSelectionCommunicator.Response(
                        request.requestId,
                        request.selectingPay,
                        asset.toAssetPayload(),
                        request.metaAccountId,
                        clearOpposite
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                showError(resourceManager.getString(R.string.swap_select_route_error))
            } finally {
                selecting.value = false
            }
        }
    }

    private class NetworkSelection(val savedNetworkIds: Set<ChainId>, val selectedNetworkIds: Set<ChainId>)
}
