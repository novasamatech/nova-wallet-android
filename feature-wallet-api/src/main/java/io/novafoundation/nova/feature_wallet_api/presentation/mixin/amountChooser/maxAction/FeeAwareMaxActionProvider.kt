package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface MaxAvailableDeduction {

    fun maxAmountDeductionFor(amountAsset: Chain.Asset): Balance
}

class ComplexFeeAwareMaxActionProvider<F : MaxAvailableDeduction>(
    feeInputMixin: FeeLoaderMixinV2<F, *>,
    inner: MaxActionProvider,
) : MaxActionProvider {

    // Fee is not deducted for display
    override val maxAvailableForDisplay: Flow<Balance?> = inner.maxAvailableForDisplay

    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?> = combine(
        inner.maxAvailableForAction,
        feeInputMixin.fee
    ) { maxAvailable, newFeeStatus ->
        if (maxAvailable == null) return@combine null

        when (newFeeStatus) {
            // do not block in case there is no fee or fee is not yet present
            FeeStatus.Error, FeeStatus.NoFee -> maxAvailable

            is FeeStatus.Loaded -> {
                val amountAsset = maxAvailable.chainAsset
                val deduction = newFeeStatus.feeModel.fee.maxAmountDeductionFor(amountAsset)

                maxAvailable - deduction
            }

            is FeeStatus.Loading -> null
        }
    }
}
