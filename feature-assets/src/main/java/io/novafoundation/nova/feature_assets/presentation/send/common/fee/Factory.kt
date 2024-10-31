package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

typealias TransferFeeLoaderMixin = FeeLoaderMixinV2.Presentation<TransferFee, TransferFeeDisplay>

context(BaseViewModel)
fun FeeLoaderMixinV2.Factory.createForTransfer(
    originChainAsset: Flow<Chain.Asset>,
    formatter: TransferFeeDisplayFormatter,
): TransferFeeLoaderMixin {
    return create(
        scope = viewModelScope,
        selectedChainAssetFlow = originChainAsset,
        feeFormatter = formatter,
        feeBalanceExtractor = TransferFeeBalanceExtractor(),
    )
}
