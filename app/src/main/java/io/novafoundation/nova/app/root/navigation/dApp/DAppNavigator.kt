package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountListFragment
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON

class DAppNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), DAppRouter {

    override fun openChangeAccount() = performNavigation(
        actionId = R.id.action_open_accounts,
        args = AccountListFragment.getBundle(AccountChosenNavDirection.BACK)
    )

    override fun openDAppBrowser() = performNavigation(R.id.action_mainFragment_to_dappBrowserGraph)

    override fun openConfirmSignExtrinsic(payload: DAppSignExtrinsicPayload) = performNavigation(
        actionId = R.id.action_DAppBrowserFragment_to_ConfirmSignExtrinsicFragment,
        args = DAppSignExtrinsicFragment.getBundle(payload)
    )
}
