package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.Companion.share
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface MaxActionProvider {

    companion object

    val maxAvailableBalance: Flow<MaxAvailableBalance>
}

fun MaxActionProvider.Companion.create(
    coroutineScope: CoroutineScope,
    builder: MaxActionProviderDsl.() -> MaxActionProvider
): MaxActionProvider {
    return builder(MaxActionProviderDsl).share(coroutineScope)
}

interface MaxActionProviderDsl {

    companion object : MaxActionProviderDsl

    fun Flow<Asset>.providingMaxOf(field: (Asset) -> Balance): MaxActionProvider {
        return AssetMaxActionProvider(this, field)
    }

    fun <F : MaxAvailableDeduction> MaxActionProvider.deductFee(
        feeLoaderMixin: FeeLoaderMixinV2<F, *>,
    ): MaxActionProvider {
        return ComplexFeeAwareMaxActionProvider(feeLoaderMixin, inner = this)
    }

    fun Flow<Chain.Asset>.providingBalance(balanceFlow: Flow<Balance>): MaxActionProvider {
        return BalanceMaxActionProvider(this, balanceFlow)
    }

    fun MaxActionProvider.share(coroutineScope: CoroutineScope): MaxActionProvider {
        return SharingMaxActionProvider(this, coroutineScope)
    }
}
