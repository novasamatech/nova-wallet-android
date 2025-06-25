package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import android.util.Log
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindGenericCall
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.isSigned
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_api.domain.multisig.bindCallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class EventsRealtimeCallDataWatcher(
    private val eventsRepository: EventsRepository,
    private val extrinsicWalk: ExtrinsicWalk,
    private val chainRegistry: ChainRegistry,
    private val multisigRepository: MultisigRepository,
    coroutineScope: CoroutineScope,
) : MultisigCallDataWatcher, CoroutineScope by coroutineScope {

    companion object {

        private const val NEW_MULTISIG = "NewMultisig"

        private const val MULTISIG_APPROVAL = "MultisigApproval"
    }

    override val callData = MutableStateFlow<Map<MultiChainCallHash, GenericCall.Instance>>(emptyMap())

    override val newMultisigEvents = MutableSharedFlow<MultiChainMultisigEvent>(extraBufferCapacity = 10)

    init {
        launch {
            val multisigChains = chainRegistry.enabledChains()
                .filter { multisigRepository.supportsMultisigSync(it) }

            multisigChains.forEach(::startDetectingNewPendingCallHashesFromEvents)
        }
    }

    private fun startDetectingNewPendingCallHashesFromEvents(chain: Chain) {
        eventsRepository.subscribeEventRecords(chain.id).map { (eventRecords, blockHash) ->
            val newMultisigEvents = eventRecords.events().findNewMultisigs(chain)
            val newCallHashes = newMultisigEvents.map { it.callHash }

            newMultisigEvents.forEach { this.newMultisigEvents.emit(it) }

            tryAddNewCallDatas(newCallHashes, blockHash, chain)
        }
            .catch { Log.e("ChainRealtimeCallDataWatcher", "Failed to detect new pending operations from events", it) }
            .launchIn(this)
    }

    private suspend fun tryAddNewCallDatas(
        newCallHashes: List<CallHash>,
        blockHash: BlockHash,
        chain: Chain
    ): Result<Unit> = runCatching {
        if (newCallHashes.isEmpty()) return@runCatching

        Log.d("ChainRealtimeCallDataWatcher", "Detected multisig at block $blockHash, trying to find call-data")

        val blockWithEvents = eventsRepository.getBlockEvents(chain.id, blockHash)

        val newCallDatas = buildMap {
            blockWithEvents.applyExtrinsic
                .filter { it.extrinsic.isSigned() }
                .forEach { extrinsicWithEvents -> extrinsicWithEvents.tryAddNewCallDatas(newCallHashes, chain) }
        }
        val newCallDatasWithChain = newCallDatas.mapKeys { (callHash, _) -> chain.id to callHash }

        Log.d("ChainRealtimeCallDataWatcher", "Found call-datas: ${newCallDatas.size}")

        callData.value += newCallDatasWithChain
    }.onFailure {
        Log.e("ChainRealtimeCallDataWatcher", "Error while detecting call-data from chain", it)
    }

    context(MutableMap<CallHash, GenericCall.Instance>)
    private suspend fun ExtrinsicWithEvents.tryAddNewCallDatas(newCallHashes: List<CallHash>, chain: Chain) {
        val visitedEntries = extrinsicWalk.walkToList(source = this@tryAddNewCallDatas, chain.id)
        visitedEntries.forEach {
            val callHashAndData = it.tryFindCallDataInAsMulti(newCallHashes, chain)
            if (callHashAndData != null) {
                put(callHashAndData)
            }
        }
    }

    private suspend fun ExtrinsicVisit.tryFindCallDataInAsMulti(
        callHashes: List<CallHash>,
        chain: Chain
    ): Pair<CallHash, GenericCall.Instance>? {
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

    private fun List<GenericEvent.Instance>.findNewMultisigs(chain: Chain): List<MultiChainMultisigEvent> {
        return findEvents(Modules.MULTISIG, NEW_MULTISIG, MULTISIG_APPROVAL).mapNotNull { newMultisigEvent ->
            extractCallHash(newMultisigEvent, chain)
        }
    }

    private fun extractCallHash(
        newMultisigEvent: GenericEvent.Instance,
        chain: Chain
    ): MultiChainMultisigEvent? {
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

            MultiChainMultisigEvent(multisig, callHash, chain.id)
        }
            .onFailure { Log.e("RealMultisigChainPendingOperationsSyncer", "Failed to parse new NewMultisig/MultisigApproval event", it) }
            .getOrNull()
    }
}
