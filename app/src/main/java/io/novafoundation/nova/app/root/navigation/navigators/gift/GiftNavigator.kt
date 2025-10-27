package io.novafoundation.nova.app.root.navigation.navigators.gift

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class GiftNavigator(
    private val commonDelegate: Navigator,
    navigationHoldersRegistry: NavigationHoldersRegistry
) : GiftRouter, BaseNavigator(navigationHoldersRegistry) {

    override fun openGiftsFlow() {
        navigationBuilder().action(R.id.action_giftsFragment_to_giftsFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectGiftAmount(assetPayload: AssetPayload) {
        commonDelegate.openSelectGiftAmount(assetPayload)
    }
}
