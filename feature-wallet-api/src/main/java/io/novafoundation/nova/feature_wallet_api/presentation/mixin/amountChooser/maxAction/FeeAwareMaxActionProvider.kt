package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.zipWithLastNonNull
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class ComplexFeeAwareMaxActionProvider<F : MaxAvailableDeduction>(
    feeMixin: FeeLoaderMixinV2<F, *>,
    inner: MaxActionProvider,
) : MaxActionProvider {

    private val usedChainAsset = inner.maxAvailableBalance.map { it.chainAsset }
        .distinctUntilChangedBy { it.fullId }

    private val lastRecordedFeeAmount = combine(usedChainAsset, feeMixin.fee) { usedChainAsset, feeStatus ->
        feeStatus.feePlanksOrNull(usedChainAsset)
    }
        .zipWithLastNonNull()
        .map { (lastNonNullFee, currentFee) ->
            // Use last non null fee while we are calculating new one
            currentFee ?: lastNonNullFee ?: BigInteger.ZERO
        }

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = combine(
        inner.maxAvailableBalance,
        lastRecordedFeeAmount
    ) { maxAvailable, lastRecordedFeeAmount ->
        val actualAvailableBalance = (maxAvailable.actualBalance - lastRecordedFeeAmount).atLeastZero()

        maxAvailable.copy(
            displayedBalance = actualAvailableBalance,
            actualBalance = actualAvailableBalance
        )
    }.distinctUntilChanged()

    private fun FeeStatus<F, *>.feePlanksOrNull(feeChainAsset: Chain.Asset): Balance? {
        if (this !is FeeStatus.Loaded) return null

        return feeModel.fee.maxAmountDeductionFor(feeChainAsset)
    }
}
