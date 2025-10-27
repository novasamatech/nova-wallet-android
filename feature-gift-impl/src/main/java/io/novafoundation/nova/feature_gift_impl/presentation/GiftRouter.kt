package io.novafoundation.nova.feature_gift_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface GiftRouter : ReturnableRouter {

    fun finishCreateGift()

    fun openGiftsFlow()

    fun openSelectGiftAmount(assetPayload: AssetPayload)
}
