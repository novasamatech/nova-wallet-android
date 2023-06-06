package io.novafoundation.nova.app.root.navigation.externalSign

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.ExternalExtrinsicDetailsFragment

class ExternalSignNavigator(
    navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), ExternalSignRouter {

    override fun openExtrinsicDetails(extrinsicContent: String) = performNavigation(
        actionId = R.id.action_ConfirmSignExtrinsicFragment_to_extrinsicDetailsFragment,
        args = ExternalExtrinsicDetailsFragment.getBundle(extrinsicContent)
    )
}
