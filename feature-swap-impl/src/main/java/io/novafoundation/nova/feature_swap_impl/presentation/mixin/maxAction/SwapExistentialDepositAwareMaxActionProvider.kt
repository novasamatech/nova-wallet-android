package io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction

import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.minus
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class SwapExistentialDepositAwareMaxActionProvider(
    private val assetOutFlow: Flow<Asset?>,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val inner: MaxActionProvider,
    chainRegistry: ChainRegistry,
) : MaxActionProvider {

    // Fee is not deducted for display
    override val maxAvailableForDisplay: Flow<Balance?> = inner.maxAvailableForDisplay

    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?> = combine(
        inner.maxAvailableForAction.filterNotNull(),
        getAssetInTotalCanDropBelowMinimumBalanceFlow(),
        assetOutSelfSufficiency()
    ) { maxAvailable, canDropBelowMinimumBalance, assetOutSelfSufficiency ->
        if (maxAvailable.chainAsset.isCommissionAsset && (canDropBelowMinimumBalance || !assetOutSelfSufficiency)) {
            val chain = chainRegistry.getChain(maxAvailable.chainAsset.chainId)
            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, maxAvailable.chainAsset)
            maxAvailable - existentialDeposit
        } else {
            maxAvailable
        }
    }

    private fun assetOutSelfSufficiency(): Flow<Boolean> {
        return assetOutFlow
            .map {
                it?.token?.configuration?.let { asset -> assetSourceRegistry.isSelfSufficientAsset(asset) }
                    .orFalse()
            }
    }

    private fun getAssetInTotalCanDropBelowMinimumBalanceFlow(): Flow<Boolean> {
        return inner.maxAvailableForAction
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.chainAsset.fullId == new.chainAsset.fullId }
            .flatMapLatest { maxAvailable ->
                assetSourceRegistry.sourceFor(maxAvailable.chainAsset)
                    .transfers
                    .totalCanDropBelowMinimumBalanceFlow(maxAvailable.chainAsset)
            }
    }
}
