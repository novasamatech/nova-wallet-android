package io.novafoundation.nova.feature_account_api.data.multisig

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import kotlinx.coroutines.flow.Flow

interface MultisigPendingOperationsService {

    context(ComputationalScope)
    fun performMultisigOperationsSync(): Flow<Unit>

    context(ComputationalScope)
    fun pendingOperationsCount(): Flow<Int>

    context(ComputationalScope)
    fun pendingOperations(): Flow<List<PendingMultisigOperation>>
}
