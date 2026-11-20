package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import android.util.Log
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.domain.config.SwapPairConfig
import io.novafoundation.nova.feature_swap_impl.data.SwapSelectionDataSource
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class SwapSelectionInteractor @Inject constructor(
    private val dataSource: SwapSelectionDataSource,
    private val globalConfigDataSource: GlobalConfigDataSource,
    private val chainRegistry: ChainRegistry,
) {

    data class Selection(val assetIn: Chain.Asset?, val assetOut: Chain.Asset?)

    suspend fun initialSelection(metaAccountId: Long): Selection {
        val chains = chainRegistry.enabledChainById()
        val saved = dataSource.get(metaAccountId)
        if (saved != null) {
            val restored = saved.resolve(chains)
            if ((saved.assetIn == null || restored.assetIn != null) && (saved.assetOut == null || restored.assetOut != null)) return restored
        }

        val remote = try {
            globalConfigDataSource.getGlobalConfig().defaultSwapPair
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Swaps", "Default swap pair unavailable; using the bundled pair", e)
            null
        }
        remote?.let {
            val selection = SwapSelectionDataSource.Selection(it.assetIn, it.assetOut).resolve(chains)
            if (selection.assetIn != null && selection.assetOut != null) return selection
        }

        return SwapSelectionDataSource.Selection(
            SwapPairConfig.Asset(Chain.Geneses.POLKADOT_ASSET_HUB, 0),
            SwapPairConfig.Asset(Chain.Geneses.POLKADOT_ASSET_HUB, 2)
        ).resolve(chains)
    }

    suspend fun resolve(asset: FullChainAssetId?): Chain.Asset? {
        return asset?.let { chainRegistry.enabledChainById()[it.chainId]?.assetsById?.get(it.assetId) }
    }

    fun remember(metaAccountId: Long, assetIn: Chain.Asset?, assetOut: Chain.Asset?) {
        dataSource.set(
            metaAccountId,
            SwapSelectionDataSource.Selection(assetIn?.configId(), assetOut?.configId())
        )
    }

    private fun Chain.Asset.configId() = SwapPairConfig.Asset(chainId, id)

    private fun SwapSelectionDataSource.Selection.resolve(chains: Map<String, Chain>) = Selection(
        assetIn?.let { chains[it.chainId]?.assetsById?.get(it.assetId) },
        assetOut?.let { chains[it.chainId]?.assetsById?.get(it.assetId) }
    )
}
