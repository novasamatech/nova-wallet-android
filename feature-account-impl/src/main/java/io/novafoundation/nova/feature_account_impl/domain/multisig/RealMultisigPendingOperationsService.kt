package io.novafoundation.nova.feature_account_impl.domain.multisig

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.data.memory.SharedComputation
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.parentCancellableFlowScope
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperationId
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val CACHE_KEY = "RealMultisigPendingOperationsService"

@FeatureScope
internal class RealMultisigPendingOperationsService @Inject constructor(
    computationalCache: ComputationalCache,
    private val syncerFactory: MultisigChainPendingOperationsSyncerFactory,
    private val accountRepository: AccountRepository,
    private val multisigRepository: MultisigRepository,
    private val chainRegistry: ChainRegistry,
) : SharedComputation(computationalCache), MultisigPendingOperationsService {

    context(ComputationalScope)
    override fun performMultisigOperationsSync(): Flow<Unit> {
        return getCachedSyncer().map { }
    }

    context(ComputationalScope)
    override fun pendingOperationsCount(): Flow<Int> {
        return getCachedSyncer().flatMapLatest { it.pendingOperationsCount }
    }

    context(ComputationalScope)
    override fun pendingOperations(): Flow<List<PendingMultisigOperation>> {
       return getCachedSyncer().flatMapLatest { it.pendingOperations }
    }

    context(ComputationalScope)
    override fun pendingOperationFlow(id: PendingMultisigOperationId): Flow<PendingMultisigOperation?> {
        return pendingOperations().map { it.findById(id) }
    }

    context(ComputationalScope)
    private fun getCachedSyncer(): Flow<MultisigPendingOperationsSyncer> {
        return cachedFlow(CACHE_KEY) {
            accountRepository.selectedMetaAccountFlow().flatMapLatest {
                parentCancellableFlowScope { scope ->
                    createSyncer(it, scope)
                }
            }
        }
    }

    private suspend fun createSyncer(account: MetaAccount, scope: CoroutineScope): MultisigPendingOperationsSyncer {
        return if (account is MultisigMetaAccount) {
            createMultisigSynced(account, scope)
        } else {
            NoOpSyncer()
        }
    }

    private suspend fun createMultisigSynced(account: MultisigMetaAccount, scope: CoroutineScope): MultisigPendingOperationsSyncer {
        val multisigChainSyncers = chainRegistry.enabledChains()
            .filter { chain -> multisigRepository.supportsMultisigSync(chain) }
            .map { chain -> syncerFactory.create(chain, account, scope) }

        return MultiChainSyncer(multisigChainSyncers, scope)
    }

    private inner class MultiChainSyncer(
        chainDelegates: List<MultisigPendingOperationsSyncer>,
        scope: CoroutineScope
    ) : MultisigPendingOperationsSyncer, CoroutineScope by scope {

        override val pendingOperationsCount: Flow<Int> = chainDelegates
            .map { it.pendingOperationsCount }
            .combine()
            .map(List<Int>::sum)
            .shareInBackground()

        override val pendingOperations: Flow<List<PendingMultisigOperation>> = chainDelegates
            .map { it.pendingOperations }
            .combine()
            .map { it.flatten() }
            .shareInBackground()
    }

    private inner class NoOpSyncer : MultisigPendingOperationsSyncer {
        override val pendingOperationsCount: Flow<Int> = flowOf(0)

        override val pendingOperations: Flow<List<PendingMultisigOperation>> = flowOf(emptyList())
    }
}
