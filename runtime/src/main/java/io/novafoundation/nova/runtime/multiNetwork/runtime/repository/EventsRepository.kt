package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.EventRecord
import io.novafoundation.nova.common.data.network.runtime.binding.Phase
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.typed.events
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

interface EventsRepository {

    /**
     * @return events in block corresponding to [blockHash] or in current block, if [blockHash] is null
     * Unparsed events are not included
     */
    suspend fun getBlockEvents(chainId: ChainId, blockHash: BlockHash? = null): BlockEvents

    suspend fun getExtrinsicWithEvents(chainId: ChainId, extrinsicHash: String, blockHash: BlockHash? = null): ExtrinsicWithEvents?
}

internal class RemoteEventsRepository(
    private val rpcCalls: RpcCalls,
    private val remoteStorageSource: StorageDataSource
) : EventsRepository {

    override suspend fun getBlockEvents(chainId: ChainId, blockHash: BlockHash?): BlockEvents {
        return remoteStorageSource.query(chainId, at = blockHash) {
            val eventRecords = metadata.system.events.query().orEmpty()
            val block = rpcCalls.getBlock(chainId, blockHash)

            BlockEvents(
                initialization = eventRecords.mapNotNull { record -> record.event.takeIf { record.phase is Phase.Initialization } },
                applyExtrinsic = groupExtrinsicWithEvents(eventRecords, block.block.extrinsics),
                finalization = eventRecords.mapNotNull { record -> record.event.takeIf { record.phase is Phase.Finalization } }
            )
        }
    }

    context(StorageQueryContext)
    private fun groupExtrinsicWithEvents(
        eventRecords: List<EventRecord>,
        extrinsics: List<String>
    ): List<ExtrinsicWithEvents> {
        val eventsByExtrinsicIndex: Map<Int, List<GenericEvent.Instance>> = eventRecords.mapNotNull { eventRecord ->
            (eventRecord.phase as? Phase.ApplyExtrinsic)?.let {
                it.extrinsicId.toInt() to eventRecord.event
            }
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        return extrinsics.mapIndexedNotNull { index, extrinsicScale ->
            val decodedExtrinsic = Extrinsic.fromHexOrNull(runtime, extrinsicScale)

            decodedExtrinsic?.let {
                val extrinsicEvents = eventsByExtrinsicIndex[index] ?: emptyList()

                ExtrinsicWithEvents(
                    extrinsic = decodedExtrinsic,
                    extrinsicHash = extrinsicScale.extrinsicHash(),
                    events = extrinsicEvents
                )
            }
        }
    }

    override suspend fun getExtrinsicWithEvents(
        chainId: ChainId,
        extrinsicHash: String,
        blockHash: BlockHash?
    ): ExtrinsicWithEvents? {
        return remoteStorageSource.query(chainId, at = blockHash) {
            val eventRecords = metadata.system.events.query().orEmpty()
            val block = rpcCalls.getBlock(chainId, blockHash)

            block.block.extrinsics.withIndex().tryFindNonNull { (index, extrinsicScale) ->
                val hash = extrinsicScale.extrinsicHash()
                if (hash != extrinsicHash) return@tryFindNonNull null

                val extrinsic = Extrinsic.fromHexOrNull(runtime, extrinsicScale) ?: return@tryFindNonNull null

                val extrinsicEvents = eventRecords.filterByExtrinsicIndex(index)

                ExtrinsicWithEvents(
                    extrinsicHash = hash,
                    extrinsic = extrinsic,
                    events = extrinsicEvents
                )
            }
        }
    }

    private fun List<EventRecord>.filterByExtrinsicIndex(index: Int): List<GenericEvent.Instance> {
        return mapNotNull { eventRecord ->
            val phase = eventRecord.phase
            if (phase !is Phase.ApplyExtrinsic) return@mapNotNull null

            val extrinsicIndex = phase.extrinsicId.toInt()
            if (extrinsicIndex != index) return@mapNotNull null

            eventRecord.event
        }
    }
}
