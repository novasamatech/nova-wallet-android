package io.novafoundation.nova.feature_multisig_operations.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsPayload

interface MultisigOperationsRouter : ReturnableRouter {

    fun openPendingOperations()

    fun openMultisigOperationDetails(payload: MultisigOperationDetailsPayload)

    fun openMultisigCallDetails(extrinsicContent: String)
}
