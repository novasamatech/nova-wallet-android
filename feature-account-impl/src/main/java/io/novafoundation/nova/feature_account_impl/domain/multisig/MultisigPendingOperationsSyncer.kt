package io.novafoundation.nova.feature_account_impl.domain.multisig


import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.LazyAsyncMultiCache
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_api.domain.multisig.bindCallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.events
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvents
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@FeatureScope
internal class MultisigChainPendingOperationsSyncerFactory @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val multisigRepository: MultisigRepository,
) {

    fun create(
        chain: Chain,
        multisig: MultisigMetaAccount,
        scope: CoroutineScope,
    ): MultisigPendingOperationsSyncer {
        return RealMultisigChainPendingOperationsSyncer(
            chain = chain,
            multisig = multisig,
            scope = scope,
            eventsRepository = eventsRepository,
            multisigRepository = multisigRepository,
        )
    }
}

internal interface MultisigPendingOperationsSyncer {

    val pendingOperationsCount: Flow<Int>

    val pendingOperations: Flow<List<PendingMultisigOperation>>
}

internal class RealMultisigChainPendingOperationsSyncer(
    val chain: Chain,
    val multisig: MultisigMetaAccount,
    val scope: CoroutineScope,
    private val eventsRepository: EventsRepository,
    private val multisigRepository: MultisigRepository,
) : CoroutineScope by scope, MultisigPendingOperationsSyncer {

    private val pendingCallHashesFlow = MutableStateFlow<List<CallHash>>(emptyList())

    private val knownCallDatas = LazyAsyncMultiCache(scope, debugLabel = "CallDataCache") {
        multisigRepository.getCallDatas(chain, it)
    }

    override val pendingOperationsCount = pendingCallHashesFlow
        .map { it.size }
        .onEach {
            Log.d("RealMultisigChainPendingOperationsSyncer", "# of operations for ${multisig.name} in ${chain.name}: $it")
        }.shareInBackground()

    override val pendingOperations = pendingCallHashesFlow.flatMapLatest(::observePendingOperations)
        .onEach(::cleanInactiveOperations)
        .map { it.values.filterNotNull() }
        .catch { Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to sync pendingOperations", it) }
        .onEach { Log.d("RealMultisigChainPendingOperationsSyncer", "Operations for ${multisig.name} in ${chain.name}: $it") }
        .shareInBackground()

    init {
        observePendingCallHashes()
    }

    private fun cleanInactiveOperations(pendingOperations: Map<CallHash, PendingMultisigOperation?>) {
        val inactiveOperations = pendingOperations.entries.mapNotNullToSet { (key, value) -> key.takeIf { value == null } }
        pendingCallHashesFlow.update { pendingCallHashes -> pendingCallHashes - inactiveOperations }
    }

    private fun observePendingCallHashes() = launchUnit {
        val accountId = multisig.accountIdKeyIn(chain) ?: return@launchUnit

        loadInitialHashes(accountId)
        startDetectingNewPendingCallHashesFromEvents(accountId)
    }

    private suspend fun observePendingOperations(callHashes: List<CallHash>): Flow<Map<CallHash, PendingMultisigOperation?>> {
        val accountId = multisig.accountIdKeyIn(chain)
        if (accountId == null || callHashes.isEmpty()) return flowOf { emptyMap() }

        val callDatasFlow = flowOf { knownCallDatas.getOrCompute(callHashes) }

        return combine(
            callDatasFlow,
            multisigRepository.subscribePendingOperations(this.chain, accountId, callHashes)
        ) { callDatas, onChainOperations ->
            onChainOperations.mapValues { (callHash, onChainOperation) ->
                PendingMultisigOperation.from(
                    onChainMultisig = onChainOperation ?: return@mapValues null,
                    callData = callDatas[callHash],
                    chain = chain,
                )
            }
        }
    }

    private fun PendingMultisigOperation.Companion.from(
        onChainMultisig: OnChainMultisig,
        callData: GenericCall.Instance?,
        chain: Chain,
    ): PendingMultisigOperation {
        return PendingMultisigOperation(
            call = callData,
            callHash = onChainMultisig.callHash,
            chain = chain,
            approvals = onChainMultisig.approvals,
            signatoryAccountId = multisig.signatoryAccountId,
            signatoryMetaId = multisig.signatoryMetaId,
            threshold = multisig.threshold,
            depositor = onChainMultisig.depositor,
            timePoint = onChainMultisig.timePoint
        )
    }

    private suspend fun loadInitialHashes(accountId: AccountIdKey) {
        runCatching {
            val pendingOperationsHashes = multisigRepository.getPendingOperationIds(chain, accountId)
            pendingCallHashesFlow.value = pendingOperationsHashes
        }.onFailure {
            Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to load initial call hashes", it)
        }
    }

    private fun startDetectingNewPendingCallHashesFromEvents(accountId: AccountIdKey) {
        eventsRepository.subscribeEventRecords(chain.id).map { eventRecords ->
            val newCallHashes = eventRecords.events().findOurNewMultisigs(accountId)
            pendingCallHashesFlow.update { it + newCallHashes }
        }
            .catch { Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to detect new pending operations from events", it) }
            .launchIn(this)
    }

    private fun List<GenericEvent.Instance>.findOurNewMultisigs(ourAccountId: AccountIdKey): List<CallHash> {
        return findEvents(Modules.MULTISIG, "NewMultisig").mapNotNull { newMultisigEvent ->
            extractOurCallHash(newMultisigEvent, ourAccountId)
        }
    }

    private fun extractOurCallHash(
        newMultisigEvent: GenericEvent.Instance,
        ourAccountId: AccountIdKey
    ): CallHash? {
        return runCatching {
            val (_, _, multisigRaw, callHashRaw) = newMultisigEvent.arguments
            val multisig = bindAccountIdKey(multisigRaw)
            val callHash = bindCallHash(callHashRaw)

            callHash.takeIf { multisig == ourAccountId }
        }.getOrNull()
    }
}
