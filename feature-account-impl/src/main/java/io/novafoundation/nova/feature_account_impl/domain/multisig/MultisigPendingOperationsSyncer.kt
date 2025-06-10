package io.novafoundation.nova.feature_account_impl.domain.multisig

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.LazyAsyncMultiCache
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.isSigned
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_api.domain.multisig.bindCallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.events
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvents
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.toByteArray
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
    private val chainRegistry: ChainRegistry,
    private val extrinsicWalk: ExtrinsicWalk,
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
            chainRegistry = chainRegistry,
            extrinsicWalk = extrinsicWalk
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
    private val extrinsicWalk: ExtrinsicWalk,
    private val chainRegistry: ChainRegistry,
) : CoroutineScope by scope, MultisigPendingOperationsSyncer {

    companion object {

        private const val NEW_MULTISIG = "NewMultisig"

        private const val MULTISIG_APPROVAL = "MultisigApproval"
    }

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

    private suspend fun observePendingOperations(callHashes: Set<CallHash>): Flow<Map<CallHash, PendingMultisigOperation?>> {
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
        eventsRepository.subscribeEventRecords(chain.id).map { (eventRecords, blockHash) ->
            val newCallHashes = eventRecords.events().findOurNewMultisigs(accountId)

            // Important to do this first, so `pendingCallHashesFlow` will trigger list re-compute when detected call-data is already present in
            // `knownCallDatas`
            tryAddNewCallDatas(newCallHashes, blockHash)

            pendingCallHashesFlow.update { it + newCallHashes }
        }
            .catch { Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to detect new pending operations from events", it) }
            .launchIn(this)
    }

    private suspend fun tryAddNewCallDatas(newCallHashes: List<CallHash>, blockHash: BlockHash): Result<Unit> = runCatching {
        if (newCallHashes.isEmpty()) return@runCatching

        Log.d("RealMultisigChainPendingOperationsSyncer", "Detected own multisig at block $blockHash, trying to find call-data")

        val blockWithEvents = eventsRepository.getBlockEvents(chain.id, blockHash)

        val newCallDatas = buildMap {
            blockWithEvents.applyExtrinsic
                .filter { it.extrinsic.isSigned() }
                .forEach { extrinsicWithEvents -> extrinsicWithEvents.tryAddNewCallDatas(newCallHashes) }
        }

        Log.d("RealMultisigChainPendingOperationsSyncer", "Found call-datas: ${newCallDatas.size}")

        knownCallDatas.putAll(newCallDatas)
    }.onFailure {
        Log.e("RealMultisigChainPendingOperationsSyncer", "Error while detecting call-data from chain", it)
    }

    context(MutableMap<CallHash, GenericCall.Instance>)
    private suspend fun ExtrinsicWithEvents.tryAddNewCallDatas(newCallHashes: List<CallHash>) {
        val visitedEntries = extrinsicWalk.walkToList(source = this@tryAddNewCallDatas, chain.id)
        visitedEntries.forEach {
            val callHashAndData = it.tryFindCallDataInAsMulti(newCallHashes)
            if (callHashAndData != null) {
                put(callHashAndData)
            }
        }
    }

    private suspend fun ExtrinsicVisit.tryFindCallDataInAsMulti(callHashes: List<CallHash>): Pair<CallHash, GenericCall.Instance>? {
        if (!call.instanceOf(Modules.MULTISIG, "as_multi")) return null

        val runtime = chainRegistry.getRuntime(chain.id)

        val callData = bindGenericCall(call.arguments["call"])
        val callHash = GenericCall.toByteArray(runtime, callData).blake2b256().intoKey()

        return if (callHash in callHashes) {
            callHash to callData
        } else {
            null
        }
    }

    private fun List<GenericEvent.Instance>.findOurNewMultisigs(ourAccountId: AccountIdKey): List<CallHash> {
        return findEvents(Modules.MULTISIG, NEW_MULTISIG, MULTISIG_APPROVAL).mapNotNull { newMultisigEvent ->
            extractOurCallHash(newMultisigEvent, ourAccountId)
        }
    }

    private fun extractOurCallHash(
        newMultisigEvent: GenericEvent.Instance,
        ourAccountId: AccountIdKey
    ): CallHash? {
        return runCatching {
            val multisigRaw: Any?
            val callHashRaw: Any?

            if (newMultisigEvent.instanceOf(Modules.MULTISIG, NEW_MULTISIG)) {
                multisigRaw = newMultisigEvent.arguments[1]
                callHashRaw = newMultisigEvent.arguments[2]
            } else {
                multisigRaw = newMultisigEvent.arguments[2]
                callHashRaw = newMultisigEvent.arguments[3]
            }

            val multisig = bindAccountIdKey(multisigRaw)
            val callHash = bindCallHash(callHashRaw)

            callHash.takeIf { multisig == ourAccountId }
        }
            .onFailure { Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to parse new NewMultisig/MultisigApproval event", it) }
            .getOrNull()
    }
}
