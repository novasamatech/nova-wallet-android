package io.novafoundation.nova.feature_account_impl.domain.multisig

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.LazyAsyncMultiCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.domain.multisig.calldata.RealtimeCallDataWatcher
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@FeatureScope
internal class MultisigChainPendingOperationsSyncerFactory @Inject constructor(
    private val multisigRepository: MultisigRepository,
) {

    fun create(
        chain: Chain,
        multisig: MultisigMetaAccount,
        callDataWatcher: RealtimeCallDataWatcher,
        scope: CoroutineScope,
    ): MultisigPendingOperationsSyncer {
        return RealMultisigChainPendingOperationsSyncer(
            chain = chain,
            multisig = multisig,
            scope = scope,
            multisigRepository = multisigRepository,
            callDataWatcher = callDataWatcher
        )
    }
}

internal interface MultisigPendingOperationsSyncer {

    val pendingOperationsCount: Flow<Int>

    val pendingOperations: Flow<List<PendingMultisigOperation>>
}

internal class RealMultisigChainPendingOperationsSyncer(
    private val chain: Chain,
    private val multisig: MultisigMetaAccount,
    private val scope: CoroutineScope,
    private val callDataWatcher: RealtimeCallDataWatcher,
    private val multisigRepository: MultisigRepository,
) : CoroutineScope by scope, MultisigPendingOperationsSyncer {

    private val pendingCallHashesFlow = MutableStateFlow<Set<CallHash>>(emptySet())

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

    private fun startDetectingNewPendingCallHashesFromEvents(accountId: AccountIdKey) {
        callDataWatcher.newMultisigEvents
            .filter { it.multisig == accountId && it.chainId == chain.id }
            .onEach {
                pendingCallHashesFlow.update { pendingCallHashesFlow -> pendingCallHashesFlow + it.callHash }
            }.launchIn(this)
    }

    private suspend fun observePendingOperations(callHashes: Set<CallHash>): Flow<Map<CallHash, PendingMultisigOperation?>> {
        val accountId = multisig.accountIdKeyIn(chain)
        if (accountId == null || callHashes.isEmpty()) return flowOf { emptyMap() }

        val callDatasFromFetchFlow = flowOf { knownCallDatas.getOrCompute(callHashes) }

        return combine(
            callDataWatcher.realtimeCallData,
            callDatasFromFetchFlow,
            multisigRepository.subscribePendingOperations(this.chain, accountId, callHashes)
        ) { realtimeCallDatas, callDatas, onChainOperations ->
            onChainOperations.mapValues { (callHash, onChainOperation) ->
                val realtimeKey = chain.id to callHash

                PendingMultisigOperation.from(
                    onChainMultisig = onChainOperation ?: return@mapValues null,
                    callData = realtimeCallDatas[realtimeKey] ?: callDatas[callHash],
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
}
