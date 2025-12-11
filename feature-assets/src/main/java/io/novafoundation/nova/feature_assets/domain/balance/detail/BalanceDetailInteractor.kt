package io.novafoundation.nova.feature_assets.domain.balance.detail

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_api.domain.AreGiftsSupportedUseCase
import io.novafoundation.nova.feature_gift_api.domain.AvailableGiftAssetsUseCase
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@ScreenScope
class BalanceDetailInteractor @Inject constructor(
    private val areGiftsSupportedUseCase: AreGiftsSupportedUseCase,
    private val giftsAvailableGiftAssetsUseCase: AvailableGiftAssetsUseCase
) {

    suspend fun areGiftSupportedForAsset(chainAsset: Chain.Asset): Boolean {
        return areGiftsSupportedUseCase.areGiftsSupported() && giftsAvailableGiftAssetsUseCase.isGiftsAvailable(chainAsset)
    }
}
