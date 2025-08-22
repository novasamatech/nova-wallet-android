package io.novafoundation.nova.feature_account_impl.domain.multisig.syncer

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.updateWithReplyCache
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.OffChainPendingMultisigOperationInfo
import io.novafoundation.nova.feature_account_impl.domain.multisig.calldata.MultisigCallDataWatcher
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimatorFromRemote
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@FeatureScope
internal class MultisigChainPendingOperationsSyncerFactory @Inject constructor(
    private val multisigRepository: MultisigRepository,
    private val operationLocalCallRepository: MultisigOperationLocalCallRepository,
    private val accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
) {

    fun create(
        chain: Chain,
        multisig: MultisigMetaAccount,
        callDataWatcher: MultisigCallDataWatcher,
        scope: CoroutineScope,
    ): MultisigPendingOperationsSyncer {
        return RealMultisigChainPendingOperationsSyncer(
            chain = chain,
            multisig = multisig,
            scope = scope,
            multisigRepository = multisigRepository,
            callDataWatcher = callDataWatcher,
            operationLocalCallRepository = operationLocalCallRepository,
            accountRepository = accountRepository,
            chainStateRepository = chainStateRepository
        )
    }
}

interface MultisigPendingOperationsSyncer {

    val pendingOperationsCount: Flow<Int>

    val pendingOperations: Flow<List<PendingMultisigOperation>>
}

internal class RealMultisigChainPendingOperationsSyncer(
    private val chain: Chain,
    private val multisig: MultisigMetaAccount,
    private val scope: CoroutineScope,
    private val callDataWatcher: MultisigCallDataWatcher,
    private val multisigRepository: MultisigRepository,
    private val accountRepository: AccountRepository,
    private val operationLocalCallRepository: MultisigOperationLocalCallRepository,
    private val chainStateRepository: ChainStateRepository,
) : CoroutineScope by scope, MultisigPendingOperationsSyncer {

    private val _pendingCallHashesFlow = singleReplaySharedFlow<Set<CallHash>>()
    private val pendingCallHashesFlow = _pendingCallHashesFlow.distinctUntilChanged()
        .shareInBackground()

    private val offChainInfos = MutableStateFlow(emptyMap<CallHash, OffChainPendingMultisigOperationInfo>())

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

        startOffChainRefreshJob()
    }

    private fun startOffChainRefreshJob() {
        pendingCallHashesFlow
            .mapLatest(::syncOffChainInfo)
            .launchIn(this)
    }

    private suspend fun syncOffChainInfo(callHashes: Set<CallHash>) {
        if (callHashes.isEmpty()) return

        val accountId = multisig.requireAccountIdKeyIn(chain)

        multisigRepository.getOffChainPendingOperationsInfo(chain, accountId, callHashes)
            .onSuccess { offChainInfos.value = it }
    }

    private suspend fun cleanInactiveOperations(pendingOperations: Map<CallHash, PendingMultisigOperation?>) {
        val inactiveOperations = pendingOperations.entries.mapNotNullToSet { (key, value) -> key.takeIf { value == null } }
        _pendingCallHashesFlow.updateWithReplyCache { pendingCallHashes -> pendingCallHashes.orEmpty() - inactiveOperations }
    }

    private fun observePendingCallHashes() = launchUnit {
        val accountId = multisig.accountIdKeyIn(chain) ?: return@launchUnit

        syncInitialHashes(accountId)
        startDetectingNewPendingCallHashesFromEvents(accountId)
    }

    private fun startDetectingNewPendingCallHashesFromEvents(accountId: AccountIdKey) {
        callDataWatcher.newMultisigEvents
            .filter { it.multisig == accountId && it.chainId == chain.id }
            .onEach {
                _pendingCallHashesFlow.updateWithReplyCache { pendingCallHashes -> pendingCallHashes.orEmpty() + it.callHash }
            }.launchIn(this)
    }

    private suspend fun observePendingOperations(callHashes: Set<CallHash>): Flow<Map<CallHash, PendingMultisigOperation?>> {
        val accountId = multisig.accountIdKeyIn(chain)
        if (accountId == null || callHashes.isEmpty()) return flowOf { emptyMap() }

        val timeEstimator = chainStateRepository.blockDurationEstimatorFromRemote(chain.id)

        return combine(
            callDataWatcher.callData,
            offChainInfos,
            multisigRepository.subscribePendingOperations(this.chain, accountId, callHashes)
        ) { realtimeCallDatas, offChainInfos, onChainOperations ->
            onChainOperations.mapValues { (callHash, onChainOperation) ->
                val realtimeKey = chain.id to callHash
                val offChainInfo = offChainInfos[callHash]

                PendingMultisigOperation.from(
                    multisigMetaId = multisig.id,
                    onChainMultisig = onChainOperation ?: return@mapValues null,
                    callData = realtimeCallDatas[realtimeKey] ?: offChainInfo?.callData,
                    chain = chain,
                    timestamp = offChainInfo?.timestamp ?: timeEstimator.timestampOf(onChainOperation.timePoint.height).milliseconds
                )
            }
        }
    }

    private fun PendingMultisigOperation.Companion.from(
        multisigMetaId: Long,
        onChainMultisig: OnChainMultisig,
        callData: GenericCall.Instance?,
        chain: Chain,
        timestamp: Duration
    ): PendingMultisigOperation {
        return PendingMultisigOperation(
            multisigMetaId = multisigMetaId,
            call = callData,
            callHash = onChainMultisig.callHash,
            chain = chain,
            approvals = onChainMultisig.approvals,
            signatoryAccountId = multisig.signatoryAccountId,
            signatoryMetaId = multisig.signatoryMetaId,
            threshold = multisig.threshold,
            depositor = onChainMultisig.depositor,
            deposit = onChainMultisig.deposit,
            timePoint = onChainMultisig.timePoint,
            timestamp = timestamp
        )
    }

    private suspend fun syncInitialHashes(accountId: AccountIdKey) {
        runCatching {
            val pendingOperationsHashes = multisigRepository.getPendingOperationIds(chain, accountId)
            removeOutdatedLocalCallHashes(pendingOperationsHashes)
            _pendingCallHashesFlow.emit(pendingOperationsHashes)
        }.onFailure {
            Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to load initial call hashes", it)
        }
    }

    private suspend fun removeOutdatedLocalCallHashes(callHashes: Set<CallHash>) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        operationLocalCallRepository.removeCallHashesExclude(
            metaAccount.id,
            chain.id,
            callHashes
        )
    }
}
