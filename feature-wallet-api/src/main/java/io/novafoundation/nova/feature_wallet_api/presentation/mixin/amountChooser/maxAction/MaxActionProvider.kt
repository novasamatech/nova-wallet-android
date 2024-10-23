package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface MaxActionProvider {

    class MaxAvailableForAction(val balance: Balance, val chainAsset: Chain.Asset)

    val maxAvailableForDisplay: Flow<Balance?>

    val maxAvailableForAction: Flow<MaxAvailableForAction?>
}

object MaxActionProviderDsl {

    fun Flow<Asset?>.providingMaxOf(field: (Asset) -> Balance, allowMaxAction: Boolean = true): MaxActionProvider {
        return AssetMaxActionProvider(this, field, allowMaxAction)
    }

    fun <F : GenericFee> MaxActionProvider.deductFee(
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
        extractTotalFee: (F) -> Balance
    ): MaxActionProvider {
        return FeeAwareMaxActionProvider(feeLoaderMixin, extractTotalFee, inner = this)
    }

    fun <F> MaxActionProvider.deductFee(
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
    ): MaxActionProvider where F : GenericFee, F : MaxAvailableDeduction {
        return MultiFeeAwareMaxActionProvider(feeLoaderMixin, inner = this)
    }
}

infix operator fun MaxActionProvider.MaxAvailableForAction?.minus(other: BigInteger?): MaxActionProvider.MaxAvailableForAction? {
    if (this == null || other == null) return null

    val difference = balance - other

    return MaxActionProvider.MaxAvailableForAction(difference.atLeastZero(), chainAsset)
}
