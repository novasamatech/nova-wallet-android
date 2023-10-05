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
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

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
}
