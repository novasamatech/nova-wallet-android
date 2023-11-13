package io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.ChainUpdateScope
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.domain.updater.AccountInfoUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.updaters.BlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.ConstantSingleChainUpdateSystem
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SupportedAssetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class SwapUpdateSystemFactory(
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val accountInfoUpdaterFactory: AccountInfoUpdaterFactory
) {

    suspend fun create(chainFlow: Flow<Chain>, coroutineScope: CoroutineScope): UpdateSystem {
        val swapSettingsState = swapSettingsStateProvider.getSwapSettingsState(coroutineScope)
        val sharedStateAdapter = SwapSharedStateAdapter(swapSettingsState, chainRegistry, coroutineScope)

        val updaters = listOf(
            blockNumberUpdater(sharedStateAdapter),
            accountInfoUpdaterFactory.create(ChainUpdateScope(chainFlow), sharedStateAdapter)
        )

        return ConstantSingleChainUpdateSystem(
            chainRegistry = chainRegistry,
            singleAssetSharedState = sharedStateAdapter,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
            updaters = updaters
        )
    }

    private fun blockNumberUpdater(sharedStateAdapter: SwapSharedStateAdapter): Updater<*> {
        return BlockNumberUpdater(chainRegistry, sharedStateAdapter, storageCache)
    }
}

/**
 * Adapter wrapper to be able to use SwapSettingsState with UpdateSystem
 */
private class SwapSharedStateAdapter(
    private val swapSettingsState: SwapSettingsState,
    private val chainRegistry: ChainRegistry,
    private val coroutineScope: CoroutineScope,
) : SelectedAssetOptionSharedState<Unit>, CoroutineScope by coroutineScope {

    override val selectedOption: Flow<SelectedAssetOptionSharedState.SupportedAssetOption<Unit>> = swapSettingsState.selectedOption
        .mapNotNull { it.assetIn }
        .distinctUntilChangedBy { it.fullId }
        .map { asset ->
            val chain = chainRegistry.getChain(asset.chainId)

            SupportedAssetOption(ChainWithAsset(chain, asset))
        }
        .shareInBackground()
}
