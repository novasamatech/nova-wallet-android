package io.novafoundation.nova.feature_gift_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_gift_impl.domain.GiftId
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface GiftRouter : ReturnableRouter {

    fun finishCreateGift()

    fun openGiftsFlow()

    fun openSelectGiftAmount(assetPayload: AssetPayload)

    fun openConfirmCreateGift(payload: CreateGiftConfirmPayload)

    fun openGiftSharing(giftId: GiftId)

    fun openMainScreen()

    fun openClaimGift(claimGiftPayload: ClaimGiftPayload)

    fun openManageWallets()
}
