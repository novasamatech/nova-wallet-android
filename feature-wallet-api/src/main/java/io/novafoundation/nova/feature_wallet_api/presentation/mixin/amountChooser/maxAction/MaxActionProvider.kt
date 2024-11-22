package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
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

    fun <F : MaxAvailableDeduction> MaxActionProvider.deductFee(
        feeLoaderMixin: FeeLoaderMixinV2<F, *>,
    ): MaxActionProvider {
        return ComplexFeeAwareMaxActionProvider(feeLoaderMixin, inner = this)
    }
}

infix operator fun MaxActionProvider.MaxAvailableForAction?.minus(other: BigInteger?): MaxActionProvider.MaxAvailableForAction? {
    if (this == null || other == null) return null

    val difference = balance - other

    return MaxActionProvider.MaxAvailableForAction(difference.atLeastZero(), chainAsset)
}
