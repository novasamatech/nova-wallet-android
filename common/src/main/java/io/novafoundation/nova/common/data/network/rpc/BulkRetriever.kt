package io.novafoundation.nova.common.data.network.rpc

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.executeAsync
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.nonNull
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.pojoList
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class GetKeysPagedRequest(
    keyPrefix: String,
    pageSize: Int,
    fullKeyOffset: String?,
) : RuntimeRequest(
    method = "state_getKeysPaged",
    params = listOfNotNull(
        keyPrefix,
        pageSize,
        fullKeyOffset,
    )
)

class GetKeys(
    keyPrefix: String,
    at: BlockHash?
) : RuntimeRequest(
    method = "state_getKeys",
    params = listOfNotNull(
        keyPrefix,
        at
    )
)

class QueryStorageAtRequest(
    keys: List<String>,
    at: String?
) : RuntimeRequest(
    method = "state_queryStorageAt",
    params = listOfNotNull(
        keys,
        at
    )
)

class QueryStorageAtResponse(
    val block: String,
    val changes: List<List<String?>>
) {
    fun changesAsMap(): Map<String, String?> {
        return changes.map { it[0]!! to it[1] }.toMap()
    }
}

class BulkRetriever(private val pageSize: Int) {

    /**
     * Retrieves all keys starting with [keyPrefix] from [at] block
     * Returns only first [defaultPageSize] elements in case historical querying is used ([at] is not null)
     */
    suspend fun retrieveAllKeys(
        socketService: SocketService,
        keyPrefix: String,
        at: BlockHash? = null
    ): List<String> = withContext(Dispatchers.IO) {
        if (at != null) {
            queryKeysByPrefixHistorical(socketService, keyPrefix, at)
        } else {
            queryKeysByPrefixCurrent(socketService, keyPrefix)
        }
    }

    suspend fun queryKeys(
        socketService: SocketService,
        keys: List<String>,
        at: BlockHash? = null
    ): Map<String, String?> = withContext(Dispatchers.IO) {
        val chunks = keys.chunked(pageSize)

        chunks.fold(mutableMapOf()) { acc, chunk ->
            ensureActive()

            val request = QueryStorageAtRequest(chunk, at)

            val chunkValues = socketService.executeAsync(request, mapper = pojoList<QueryStorageAtResponse>().nonNull())
                .first().changesAsMap()

            acc.putAll(chunkValues)

            acc
        }
    }

    /**
     * Note: the amount of keys returned by this method is limited by [defaultPageSize]
     * So it is should not be used for storages with big amount of entries
     */
    private suspend fun queryKeysByPrefixHistorical(
        socketService: SocketService,
        prefix: String,
        at: BlockHash
    ): List<String> {
        // We use `state_getKeys` for historical prefix queries instead of `state_getKeysPaged`
        // since most of the chains always return empty list when the same is requested via `state_getKeysPaged`
        // Thus, we can only request up to 1000 first historical keys
        val request = GetKeys(prefix, at)

        return socketService.executeAsync(request, mapper = pojoList<String>().nonNull())
    }

    private suspend fun queryKeysByPrefixCurrent(
        socketService: SocketService,
        prefix: String
    ): List<String> {
        val result = mutableListOf<String>()

        var currentOffset: String? = null

        while (true) {
            coroutineContext.ensureActive()

            val request = GetKeysPagedRequest(prefix, pageSize, currentOffset)

            val page = socketService.executeAsync(request, mapper = pojoList<String>().nonNull())

            result += page

            if (isLastPage(page)) break

            currentOffset = page.last()
        }

        return result
    }

    private fun isLastPage(page: List<String>) = page.size < pageSize
}

suspend fun BulkRetriever.queryKey(
    socketService: SocketService,
    key: String,
    at: BlockHash? = null
): String? = queryKeys(socketService, listOf(key), at).values.first()
