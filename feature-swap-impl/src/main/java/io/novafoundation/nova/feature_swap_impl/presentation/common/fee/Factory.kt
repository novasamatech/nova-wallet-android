package io.novafoundation.nova.feature_swap_impl.presentation.common.fee

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

typealias SwapFeeLoaderMixin = FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>

context(BaseViewModel)
fun FeeLoaderMixinV2.Factory.createForSwap(
    chainAssetIn: Flow<Chain.Asset>,
    interactor: SwapInteractor,
    configuration: FeeLoaderMixinV2.Configuration<SwapFee, FeeDisplay> = FeeLoaderMixinV2.Configuration()
): SwapFeeLoaderMixin {
    return create(
        scope = viewModelScope,
        feeContextFlow = chainAssetIn.asFeeContextFromChain(),
        feeFormatter = SwapFeeFormatter(interactor),
        feeInspector = SwapFeeInspector(),
        configuration = configuration
    )
}
