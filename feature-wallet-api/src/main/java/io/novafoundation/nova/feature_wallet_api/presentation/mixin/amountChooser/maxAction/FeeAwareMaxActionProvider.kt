package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import androidx.lifecycle.asFlow
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface MaxAvailableDeduction {

    fun deductionFor(amountAsset: Chain.Asset): Balance
}

class ComplexFeeAwareMaxActionProvider<F>(
    feeInputMixin: GenericFeeLoaderMixin<F>,
    inner: MaxActionProvider,
) : MaxActionProvider where F : Fee, F : MaxAvailableDeduction {

    // Fee is not deducted for display
    override val maxAvailableForDisplay: Flow<Balance?> = inner.maxAvailableForDisplay

    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?> = combine(
        inner.maxAvailableForAction,
        feeInputMixin.feeLiveData.asFlow()
    ) { maxAvailable, newFeeStatus ->
        if (maxAvailable == null) return@combine null

        when (newFeeStatus) {
            // do not block in case there is no fee or fee is not yet present
            FeeStatus.Error, FeeStatus.NoFee -> maxAvailable

            is FeeStatus.Loaded -> {
                val amountAsset = maxAvailable.chainAsset
                val deduction = newFeeStatus.feeModel.fee.deductionFor(amountAsset)

                maxAvailable - deduction
            }

            FeeStatus.Loading -> null
        }
    }
}
