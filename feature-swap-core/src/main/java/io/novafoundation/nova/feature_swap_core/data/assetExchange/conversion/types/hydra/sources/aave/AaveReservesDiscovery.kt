package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.assetLocations
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.assetRegistry
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

/**
 * Hydration's Aave v3 pool. The runtime keeps the same address as a constant (`Liquidation::BorrowingContract` default).
 */
private const val AAVE_POOL_ADDRESS = "0x1b02E051683b5cfaC5929C25E84adb26ECf87B38"

private const val ADDRESS_HEX_LENGTH = 40

// Hydration exposes every registry asset as an ERC20 precompile at 0x00..01 followed by the big-endian asset id
private const val ASSET_PRECOMPILE_PREFIX = "000000000000000000000000000000" + "01"

/**
 * Finds Aave reserve <-> aToken pairs by reading the Aave pool contract directly.
 *
 * A fallback for `AaveTradeExecutor_pairs`, which Hydration's runtime answers with an empty list once
 * `getReservesList()` no longer fits its view gas limit - the case since block 14672011 (2026-09-16).
 */
class AaveReservesDiscovery @Inject constructor(
    private val chainRegistry: ChainRegistry,
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorage: StorageDataSource,
) {

    private val reserveDetailsSemaphore = Semaphore(RESERVE_DETAILS_CONCURRENCY)

    internal suspend fun discoverPairs(chainId: ChainId): List<AavePoolPair> = coroutineScope {
        val socket = chainRegistry.getSocket(chainId)
        val reserves = AavePoolAbi.decodeReservesList(ethCall(socket, AavePoolAbi.getReservesListCall()))
        val contractAssetIds = contractAssetIds(chainId)

        reserves.map { reserve ->
            async {
                reserveDetailsSemaphore.withPermit {
                    val reserveData = ethCall(socket, AavePoolAbi.getReserveDataCall(reserve))
                    val aToken = AavePoolAbi.decodeATokenAddress(reserveData)

                    val reserveId = assetIdOf(reserve, contractAssetIds) ?: return@withPermit null
                    val aTokenId = assetIdOf(aToken, contractAssetIds) ?: return@withPermit null

                    AavePoolPair(reserveId, aTokenId)
                }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun ethCall(socket: SocketService, data: String): String {
        val request = RuntimeRequest(
            method = "eth_call",
            params = listOf(mapOf("to" to AAVE_POOL_ADDRESS, "data" to data), "latest")
        )

        return socket.executeAsync(request, mapper = pojo<String>().nonNull()).removePrefix("0x")
    }

    /** ERC20-bound registry assets (aTokens and the like), keyed by lowercase contract address without 0x. */
    private suspend fun contractAssetIds(chainId: ChainId): Map<String, HydraDxAssetId> {
        val locations = remoteStorage.query(chainId) {
            metadata.assetRegistry.assetLocations.entries()
        }

        return locations.entries
            .mapNotNull { (assetId, contract) -> contract?.lowercase()?.let { it to assetId } }
            .toMap()
    }

    private fun assetIdOf(address: String, contractAssetIds: Map<String, HydraDxAssetId>): HydraDxAssetId? {
        val normalized = address.removePrefix("0x").lowercase()

        return if (normalized.length == ADDRESS_HEX_LENGTH && normalized.startsWith(ASSET_PRECOMPILE_PREFIX)) {
            BigInteger(normalized.removePrefix(ASSET_PRECOMPILE_PREFIX), 16)
        } else {
            contractAssetIds[normalized]
        }
    }
}

internal data class AavePoolPair(val firstAsset: HydraDxAssetId, val secondAsset: HydraDxAssetId)

internal object AavePoolAbi {

    private const val SELECTOR_GET_RESERVES_LIST = "0xd1946dbc"
    private const val SELECTOR_GET_RESERVE_DATA = "0x35ea6a75"

    private const val ABI_WORD_BYTES = 32
    private const val ABI_WORD_HEX_LENGTH = ABI_WORD_BYTES * 2

    // ReserveData(configuration, liquidityIndex, currentLiquidityRate, variableBorrowIndex, currentVariableBorrowRate,
    // currentStableBorrowRate, lastUpdateTimestamp, id, aTokenAddress, ...)
    private const val RESERVE_DATA_ATOKEN_WORD = 8

    fun getReservesListCall(): String = SELECTOR_GET_RESERVES_LIST

    fun getReserveDataCall(reserve: String): String {
        val normalized = reserve.removePrefix("0x").lowercase()
        require(normalized.length == ADDRESS_HEX_LENGTH && normalized.all { it.isHexDigit() }) {
            "Invalid Aave reserve address: $reserve"
        }

        return SELECTOR_GET_RESERVE_DATA + normalized.padStart(ABI_WORD_HEX_LENGTH, '0')
    }

    fun decodeATokenAddress(response: String): String {
        return response.normalizedHex().word(RESERVE_DATA_ATOKEN_WORD).decodeAddressWord()
    }

    fun decodeReservesList(response: String): List<String> {
        val normalized = response.normalizedHex()

        // Dynamic ABI value: the first word points to the array, followed by its length and elements.
        val arrayOffsetBytes = normalized.word(0).toInt(16)
        require(arrayOffsetBytes % ABI_WORD_BYTES == 0) { "Invalid Aave ABI array offset: $arrayOffsetBytes" }

        val lengthWordIndex = arrayOffsetBytes / ABI_WORD_BYTES
        val arrayLength = normalized.word(lengthWordIndex).toInt(16)

        return (0 until arrayLength).map { normalized.word(lengthWordIndex + 1 + it).decodeAddressWord() }
    }

    private fun String.normalizedHex(): String {
        return removePrefix("0x").lowercase()
    }

    private fun String.word(index: Int): String {
        val fromIndex = index * ABI_WORD_HEX_LENGTH
        val toIndex = fromIndex + ABI_WORD_HEX_LENGTH
        require(length >= toIndex) { "Invalid Aave ABI response: word $index is missing" }

        return substring(fromIndex, toIndex)
    }

    private fun String.decodeAddressWord(): String {
        return takeLast(ADDRESS_HEX_LENGTH)
    }

    private fun Char.isHexDigit(): Boolean = isDigit() || lowercaseChar() in 'a'..'f'
}

private const val RESERVE_DETAILS_CONCURRENCY = 6
