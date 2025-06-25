package io.novafoundation.nova.app.root.navigation.navigators.multisig

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.ExternalExtrinsicDetailsFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallPayload

class MultisigOperationsNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonDelegate: Navigator,
) : BaseNavigator(navigationHoldersRegistry), MultisigOperationsRouter {

    override fun openPendingOperations() {
        navigationBuilder().action(R.id.action_multisigCreatedDialog_to_multisigPendingOperationsFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openMain() {
        commonDelegate.openMain()
    }

    override fun openMultisigOperationDetails(payload: MultisigOperationDetailsPayload) {
        navigationBuilder().action(R.id.action_multisigPendingOperationsFragment_to_multisigOperationDetailsFragment)
            .setArgs(MultisigOperationDetailsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openMultisigCallDetails(extrinsicContent: String) {
        navigationBuilder().action(R.id.action_multisigOperationDetailsFragment_to_externalExtrinsicDetailsFragment)
            .setArgs(ExternalExtrinsicDetailsFragment.getBundle(extrinsicContent))
            .navigateInFirstAttachedContext()
    }

    override fun openEnterCallDetails(payload: MultisigOperationEnterCallPayload) {
        navigationBuilder().action(R.id.action_multisigOperationDetailsFragment_to_enterCallDetails)
            .setArgs(MultisigOperationEnterCallFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }
}
