package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import androidx.lifecycle.asFlow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FeeAwareMaxActionProvider<F : GenericFee>(
    feeInputMixin: GenericFeeLoaderMixin<F>,
    private val extractTotalFee: (F) -> Balance,
    inner: MaxActionProvider,
) : MaxActionProvider {

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
                val feeAsset = newFeeStatus.feeModel.chainAsset
                val amountAsset = maxAvailable.chainAsset

                if (feeAsset.fullId == amountAsset.fullId) {
                    val genericFee = newFeeStatus.feeModel.decimalFee.genericFee
                    val extractedFee = extractTotalFee(genericFee)

                    maxAvailable - extractedFee
                } else {
                    maxAvailable
                }
            }

            FeeStatus.Loading -> null
        }
    }
}

interface MaxAvailableDeduction {

    fun deductionFor(amountAsset: Chain.Asset): Balance
}

class MultiFeeAwareMaxActionProvider<F>(
    feeInputMixin: GenericFeeLoaderMixin<F>,
    inner: MaxActionProvider,
) : MaxActionProvider where F : GenericFee, F : MaxAvailableDeduction {

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
                val deduction = newFeeStatus.feeModel.decimalFee.genericFee.deductionFor(amountAsset)

                maxAvailable - deduction
            }

            FeeStatus.Loading -> null
        }
    }
}
