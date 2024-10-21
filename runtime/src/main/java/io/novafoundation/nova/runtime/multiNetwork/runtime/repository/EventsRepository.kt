package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.EventRecord
import io.novafoundation.nova.common.data.network.runtime.binding.Phase
import io.novafoundation.nova.common.data.network.runtime.binding.bindEventRecords
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

interface EventsRepository {

    /**
     * @return events in block corresponding to [blockHash] or in current block, if [blockHash] is null
     * Unparsed events are not included
     */
    suspend fun getEventsInBlock(chainId: ChainId, blockHash: BlockHash? = null): List<EventRecord>

    /**
     * @return extrinsics with their event in block corresponding to [blockHash] or in current block, if [blockHash] is null
     * Unparsed events & extrinsics are not included
     */
    suspend fun getExtrinsicsWithEvents(chainId: ChainId, blockHash: BlockHash? = null): List<ExtrinsicWithEvents>

    suspend fun getExtrinsicWithEvents(chainId: ChainId, extrinsicHash: String, blockHash: BlockHash? = null): ExtrinsicWithEvents?
}

class InherentEvents(
    val initialization: List<GenericEvent.Instance>,
    val finalization: List<GenericEvent.Instance>
)

suspend fun EventsRepository.getInherentEvents(chainId: ChainId, at: BlockHash) : InherentEvents {
    val allEvents = getEventsInBlock(chainId, at)

    return InherentEvents(
        initialization = allEvents.mapNotNull { record -> record.event.takeIf { record.phase is Phase.Initialization } },
        finalization = allEvents.mapNotNull { record -> record.event.takeIf { record.phase is Phase.Finalization } },
    )
}

class RemoteEventsRepository(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val remoteStorageSource: StorageDataSource
) : EventsRepository {

    override suspend fun getEventsInBlock(chainId: ChainId, blockHash: BlockHash?): List<EventRecord> {
        return remoteStorageSource.queryNonNull(
            chainId = chainId,
            keyBuilder = { it.metadata.system().storage("Events").storageKey() },
            binding = { scale, runtime ->
                bindEventRecords(scale, runtime)
            },
            at = blockHash
        )
    }

    override suspend fun getExtrinsicsWithEvents(
        chainId: ChainId,
        blockHash: BlockHash?
    ): List<ExtrinsicWithEvents> {
        val runtime = chainRegistry.getRuntime(chainId)

        val block = rpcCalls.getBlock(chainId, blockHash)
        val events = getEventsInBlock(chainId, blockHash)

        val eventsByExtrinsicIndex: Map<Int, List<GenericEvent.Instance>> = events.mapNotNull { eventRecord ->
            (eventRecord.phase as? Phase.ApplyExtrinsic)?.let {
                it.extrinsicId.toInt() to eventRecord.event
            }
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        return block.block.extrinsics.mapIndexed { index, extrinsicScale ->
            val decodedExtrinsic = Extrinsic.fromHexOrNull(runtime, extrinsicScale)

            decodedExtrinsic?.let {
                val extrinsicEvents = eventsByExtrinsicIndex[index] ?: emptyList()

                ExtrinsicWithEvents(
                    extrinsic = decodedExtrinsic,
                    extrinsicHash = extrinsicScale.extrinsicHash(),
                    events = extrinsicEvents
                )
            }
        }.filterNotNull()
    }

    override suspend fun getExtrinsicWithEvents(
        chainId: ChainId,
        extrinsicHash: String,
        blockHash: BlockHash?
    ): ExtrinsicWithEvents? {
        val runtime = chainRegistry.getRuntime(chainId)

        val block = rpcCalls.getBlock(chainId, blockHash)
        val events = getEventsInBlock(chainId, blockHash)

        return block.block.extrinsics.withIndex().tryFindNonNull { (index, extrinsicScale) ->
            val hash = extrinsicScale.extrinsicHash()
            if (hash != extrinsicHash) return@tryFindNonNull null

            val extrinsic = Extrinsic.fromHexOrNull(runtime, extrinsicScale) ?: return@tryFindNonNull null

            val extrinsicEvents = events.findByExtrinsicIndex(index)

            ExtrinsicWithEvents(
                extrinsicHash = hash,
                extrinsic = extrinsic,
                events = extrinsicEvents
            )
        }
    }

    private fun List<EventRecord>.findByExtrinsicIndex(index: Int): List<GenericEvent.Instance> {
        return mapNotNull { eventRecord ->
            val phase = eventRecord.phase
            if (phase !is Phase.ApplyExtrinsic) return@mapNotNull null

            val extrinsicIndex = phase.extrinsicId.toInt()
            if (extrinsicIndex != index) return@mapNotNull null

            eventRecord.event
        }
    }
}
