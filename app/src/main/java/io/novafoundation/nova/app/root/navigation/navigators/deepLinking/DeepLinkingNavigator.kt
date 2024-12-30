package io.novafoundation.nova.app.root.navigation.navigators.deepLinking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.openSplitScreenWithInstantAction
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class DeepLinkingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val dAppRouter: DAppRouter,
    private val accountRouter: AccountRouter,
    private val assetsRouter: AssetsRouter
) : BaseNavigator(navigationHoldersRegistry), DeepLinkingRouter {

    override fun openAssetDetails(payload: AssetPayload) {
        openSplitScreenWithInstantAction(R.id.action_mainFragment_to_balanceDetailFragment, BalanceDetailFragment.getBundle(payload))
    }

    override fun openDAppBrowser(url: String) {
        dAppRouter.openDAppBrowser(DAppBrowserPayload.Address(url))
    }

    override fun openImportAccountScreen(payload: ImportAccountPayload) {
        accountRouter.openImportAccountScreen(payload)
    }

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        openSplitScreenWithInstantAction(R.id.action_open_referendum_details, ReferendumDetailsFragment.getBundle(payload))
    }

    override fun openStakingDashboard() {
        assetsRouter.openStaking()
    }
}
