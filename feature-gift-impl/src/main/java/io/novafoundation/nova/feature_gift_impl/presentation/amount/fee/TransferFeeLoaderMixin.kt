package io.novafoundation.nova.feature_gift_impl.presentation.amount.fee

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

typealias GiftFeeLoaderMixin = FeeLoaderMixinV2.Presentation<GiftFee, FeeDisplay>

context(BaseViewModel)
fun FeeLoaderMixinV2.Factory.createForGifts(
    originChainAsset: Flow<Chain.Asset>,
    formatter: GiftFeeDisplayFormatter
): GiftFeeLoaderMixin {
    return create(
        scope = viewModelScope,
        feeContextFlow = originChainAsset.asFeeContextFromSelf(),
        feeFormatter = formatter,
        feeInspector = GiftFeeInspector(),
        configuration = FeeLoaderMixinV2.Configuration()
    )
}
