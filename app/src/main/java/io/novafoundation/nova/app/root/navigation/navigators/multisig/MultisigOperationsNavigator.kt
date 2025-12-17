package io.novafoundation.nova.app.root.navigation.navigators.multisig

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.MultisigOperationDetailsFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.full.MultisigOperationFullDetailsFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallFragment

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
        navigationBuilder().cases()
            .addCase(R.id.multisigPendingOperationsFragment, R.id.action_multisigPendingOperationsFragment_to_multisigOperationDetailsFragment)
            .setFallbackCase(R.id.action_multisigOperationDetailsFragment)
            .setArgs(MultisigOperationDetailsFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openMultisigFullDetails(payload: MultisigOperationPayload) {
        navigationBuilder().action(R.id.action_multisigOperationDetailsFragment_to_externalExtrinsicDetailsFragment)
            .setArgs(MultisigOperationFullDetailsFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openEnterCallDetails(payload: MultisigOperationPayload) {
        navigationBuilder().action(R.id.action_multisigOperationDetailsFragment_to_enterCallDetails)
            .setArgs(MultisigOperationEnterCallFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }
}
