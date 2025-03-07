package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

class ExistentialDepositDeductionMaxActionProvider(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val inner: MaxActionProvider,
    private val deductEdFlow: Flow<Boolean>
) : MaxActionProvider {

    private val usedChainAsset = inner.maxAvailableBalance.map { it.chainAsset }
        .distinctUntilChangedBy { it.fullId }

    private val existentialDeposit = usedChainAsset.map {
        assetSourceRegistry.existentialDepositInPlanks(it)
    }

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = combine(
        inner.maxAvailableBalance,
        existentialDeposit,
        deductEdFlow
    ) { maxAvailable, existentialDeposit, deductEd ->
        if (!deductEd) return@combine maxAvailable

        val actualAvailableBalance = (maxAvailable.actualBalance - existentialDeposit).atLeastZero()

        maxAvailable.copy(
            displayedBalance = actualAvailableBalance,
            actualBalance = actualAvailableBalance
        )
    }.distinctUntilChanged()
}
