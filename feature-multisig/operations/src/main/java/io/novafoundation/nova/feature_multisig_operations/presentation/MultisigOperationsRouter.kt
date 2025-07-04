package io.novafoundation.nova.feature_multisig_operations.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallPayload

interface MultisigOperationsRouter : ReturnableRouter {

    fun openPendingOperations()

    fun openMain()

    fun openMultisigOperationDetails(payload: MultisigOperationDetailsPayload)

    fun openMultisigFullDetails(payload: MultisigOperationDetailsPayload)

    fun openEnterCallDetails(payload: MultisigOperationEnterCallPayload)
}
