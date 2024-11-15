package io.novafoundation.nova.feature_swap_impl.presentation.common.mixin.maxAction

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.deductFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.providingMaxOf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import kotlinx.coroutines.flow.Flow

class MaxActionProviderFactory {

    fun <F : MaxAvailableDeduction> create(
        assetInFlow: Flow<Asset?>,
        feeLoaderMixin: FeeLoaderMixinV2<F, *>,
    ): MaxActionProvider {
        return assetInFlow.providingMaxOf(Asset::transferableInPlanks, allowMaxAction=true)
            .deductFee(feeLoaderMixin)
    }
}
