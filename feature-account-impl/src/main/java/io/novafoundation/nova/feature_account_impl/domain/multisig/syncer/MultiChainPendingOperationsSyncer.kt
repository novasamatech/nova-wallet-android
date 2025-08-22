package io.novafoundation.nova.feature_account_impl.domain.multisig.syncer

import io.novafoundation.nova.common.utils.accumulate
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MultiChainSyncer(
    chainDelegates: Map<ChainId, MultisigPendingOperationsSyncer>,
    scope: CoroutineScope
) : MultisigPendingOperationsSyncer, CoroutineScope by scope {

    override val pendingOperationsCount: Flow<Int> = chainDelegates.values
        .map { it.pendingOperationsCount }
        .accumulate()
        .map { it.sum() }
        .shareInBackground()

    override val pendingOperations: Flow<List<PendingMultisigOperation>> = chainDelegates.values
        .map { it.pendingOperations }
        .accumulate()
        .map { it.flatten() }
        .shareInBackground()
}

class NoOpSyncer : MultisigPendingOperationsSyncer {
    override val pendingOperationsCount: Flow<Int> = flowOf(0)

    override val pendingOperations: Flow<List<PendingMultisigOperation>> = flowOf(emptyList())
}
