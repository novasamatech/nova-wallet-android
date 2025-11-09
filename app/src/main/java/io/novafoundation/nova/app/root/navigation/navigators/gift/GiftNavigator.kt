package io.novafoundation.nova.app.root.navigation.navigators.gift

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_gift_impl.domain.GiftId
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftFragment
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmFragment
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmPayload
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftFragment
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class GiftNavigator(
    private val commonDelegate: Navigator,
    navigationHoldersRegistry: NavigationHoldersRegistry
) : GiftRouter, BaseNavigator(navigationHoldersRegistry) {

    override fun finishCreateGift() {
        navigationBuilder().action(R.id.action_finishCreateGift)
            .navigateInFirstAttachedContext()
    }

    override fun openGiftsFlow() {
        navigationBuilder().action(R.id.action_giftsFragment_to_giftsFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectGiftAmount(assetPayload: AssetPayload) {
        commonDelegate.openSelectGiftAmount(assetPayload)
    }

    override fun openConfirmCreateGift(payload: CreateGiftConfirmPayload) {
        navigationBuilder().action(R.id.action_openConfirmCreateGift)
            .setArgs(CreateGiftConfirmFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openGiftSharing(giftId: GiftId, isSecondOpen: Boolean) {
        navigationBuilder().action(R.id.action_openShareGiftFragment)
            .setArgs(ShareGiftFragment.createPayload(ShareGiftPayload(giftId, isSecondOpen)))
            .navigateInFirstAttachedContext()
    }

    override fun openMainScreen() {
        commonDelegate.openMain()
    }

    override fun openClaimGift(payload: ClaimGiftPayload) {
        navigationBuilder().action(R.id.action_openClaimGiftFragment)
            .setArgs(ClaimGiftFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openManageWallets() {
        commonDelegate.openWallets()
    }
}
