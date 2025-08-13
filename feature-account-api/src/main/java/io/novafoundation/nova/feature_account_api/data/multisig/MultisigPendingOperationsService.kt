package io.novafoundation.nova.feature_account_api.data.multisig

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperationId
import kotlinx.coroutines.flow.Flow

interface MultisigPendingOperationsService {

    context(ComputationalScope)
    fun operationAvailableFlow(id: PendingMultisigOperationId): Flow<Boolean>

    context(ComputationalScope)
    fun performMultisigOperationsSync(): Flow<Unit>

    context(ComputationalScope)
    fun pendingOperationsCountFlow(): Flow<Int>

    context(ComputationalScope)
    suspend fun getPendingOperationsCount(): Int

    context(ComputationalScope)
    fun pendingOperations(): Flow<List<PendingMultisigOperation>>

    context(ComputationalScope)
    fun pendingOperationFlow(id: PendingMultisigOperationId): Flow<PendingMultisigOperation?>

    context(ComputationalScope)
    suspend fun pendingOperation(id: PendingMultisigOperationId): PendingMultisigOperation?
}
