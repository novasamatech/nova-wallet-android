package io.novafoundation.nova.feature_multisig_operations.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.MultisigOperationDetailsPayload

interface MultisigOperationsRouter : ReturnableRouter {

    fun openPendingOperations()

    fun openMain()

    fun openMultisigOperationDetails(payload: MultisigOperationDetailsPayload)

    fun openMultisigFullDetails(payload: MultisigOperationPayload)

    fun openEnterCallDetails(payload: MultisigOperationPayload)
}
