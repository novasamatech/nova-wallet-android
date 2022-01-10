package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountListFragment
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.extrinsicDetails.DappExtrinsicDetailsFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment

class DAppNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), DAppRouter {

    override fun openChangeAccount() = performNavigation(
        actionId = R.id.action_open_accounts,
        args = AccountListFragment.getBundle(AccountChosenNavDirection.BACK)
    )

    override fun openDAppBrowser(initialUrl: String) = performNavigation(
        actionId = R.id.action_mainFragment_to_dappBrowserGraph,
        args = DAppBrowserFragment.getBundle(initialUrl)
    )

    override fun openExtrinsicDetails(extrinsicContent: String) = performNavigation(
        actionId = R.id.action_ConfirmSignExtrinsicFragment_to_extrinsicDetailsFragment,
        args = DappExtrinsicDetailsFragment.getBundle(extrinsicContent)
    )
}
