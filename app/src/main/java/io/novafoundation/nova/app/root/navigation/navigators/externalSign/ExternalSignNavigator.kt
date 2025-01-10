package io.novafoundation.nova.app.root.navigation.navigators.externalSign

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.ExternalExtrinsicDetailsFragment

class ExternalSignNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), ExternalSignRouter {

    override fun openExtrinsicDetails(extrinsicContent: String) {
        navigationBuilder(R.id.action_ConfirmSignExtrinsicFragment_to_extrinsicDetailsFragment)
            .setArgs(ExternalExtrinsicDetailsFragment.getBundle(extrinsicContent))
            .navigateInFirstAttachedContext()
    }
}
