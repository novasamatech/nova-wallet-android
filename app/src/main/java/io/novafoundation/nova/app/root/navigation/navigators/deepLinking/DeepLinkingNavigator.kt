package io.novafoundation.nova.app.root.navigation.navigators.deepLinking

import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

class DeepLinkingNavigator(
    private val accountRouter: AccountRouter,
    private val assetsRouter: AssetsRouter,
    private val dAppRouter: DAppRouter,
    private val governanceRouter: GovernanceRouter
) : DeepLinkingRouter {

    override fun openAssetDetails(payload: AssetPayload) {
        assetsRouter.openAssetDetails(payload)
    }

    override fun openDAppBrowser(url: String) {
        dAppRouter.openDAppBrowser(DAppBrowserPayload.Address(url))
    }

    override fun openImportAccountScreen(importAccountPayload: ImportAccountPayload) {
        accountRouter.openImportAccountScreen(importAccountPayload)
    }

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        governanceRouter.openReferendum(payload)
    }

    override fun openStakingDashboard() {
        assetsRouter.openStaking()
    }
}
