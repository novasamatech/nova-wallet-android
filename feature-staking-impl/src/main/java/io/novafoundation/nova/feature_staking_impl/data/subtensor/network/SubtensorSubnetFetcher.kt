package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorSubnetInfo
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b128Concat
import io.novasama.substrate_sdk_android.hash.Hasher.xxHash128
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigInteger

/**
 * Fetches subnet metadata for the Bittensor (TAO) chain via direct HTTP
 * JSON-RPC. Queries `SubtensorModule.{SubnetTAO, SubnetAlphaIn,
 * SubnetIdentitiesV3}` for netuids 1..128 in a single batch request.
 *
 * Mirrors iOS `SubtensorSubnetFetcher.swift`. SubnetTAO + SubnetAlphaIn
 * use the `identity` hasher (raw u16 LE netuid as the storage key), and
 * SubnetIdentitiesV3 uses `blake2b128Concat`. The subnet name is the
 * first SCALE-encoded `Vec<u8>` field of the V3 identity record.
 */
class SubtensorSubnetFetcher(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {

    private val rpcUrl: String = SubtensorStakingConstants.FALLBACK_RPC_URL
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun fetchAllSubnets(): List<SubtensorSubnetInfo> {
        val moduleHash = "SubtensorModule".toByteArray().xxHash128()
        val taoItemHash = "SubnetTAO".toByteArray().xxHash128()
        val alphaItemHash = "SubnetAlphaIn".toByteArray().xxHash128()
        val identitiesItemHash = "SubnetIdentitiesV3".toByteArray().xxHash128()

        val requests = mutableListOf<Map<String, Any>>()
        for (netuid in 1..128) {
            val netuidLE = u16LE(netuid)
            val taoKey = "0x" + (moduleHash + taoItemHash + netuidLE).toHex()
            val alphaKey = "0x" + (moduleHash + alphaItemHash + netuidLE).toHex()
            val identityKey = "0x" + (moduleHash + identitiesItemHash + netuidLE.blake2b128Concat()).toHex()

            val baseId = netuid * 3
            requests += rpcRequest(baseId, "state_getStorage", listOf(taoKey))
            requests += rpcRequest(baseId + 1, "state_getStorage", listOf(alphaKey))
            requests += rpcRequest(baseId + 2, "state_getStorage", listOf(identityKey))
        }

        val responses = sendBatch(requests)

        val results = mutableListOf<SubtensorSubnetInfo>()
        for (netuid in 1..128) {
            val baseId = netuid * 3
            val taoHex = responses[baseId]
            val alphaHex = responses[baseId + 1]
            val identityHex = responses[baseId + 2]

            val taoReserve = decodeU64Le(taoHex)
            val alphaIn = decodeU64Le(alphaHex)

            // iOS skips netuids whose SubnetTAO is zero — those subnets are
            // either not active or the AMM has been drained, so a stake on
            // them would have no liquidity. Match that filter.
            if (taoReserve.signum() <= 0) continue

            results += SubtensorSubnetInfo(
                netuid = netuid,
                name = decodeSubnetName(identityHex),
                taoReserve = taoReserve,
                alphaInReserve = alphaIn,
            )
        }
        return results
    }

    private fun rpcRequest(id: Int, method: String, params: List<Any>): Map<String, Any> = mapOf(
        "jsonrpc" to "2.0",
        "id" to id,
        "method" to method,
        "params" to params,
    )

    private fun sendBatch(requests: List<Map<String, Any>>): Map<Int, String?> {
        val body = gson.toJson(requests).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(rpcUrl)
            .post(body)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyMap()
            val text = response.body?.string() ?: return emptyMap()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val parsed: List<Map<String, Any>> = gson.fromJson(text, type) ?: return emptyMap()
            parsed.mapNotNull { item ->
                val rpcId = (item["id"] as? Number)?.toInt() ?: return@mapNotNull null
                rpcId to (item["result"] as? String)
            }.toMap()
        }
    }

    /**
     * Single-subnet variant. Used by the stake-confirm flow to read the
     * live AMM spot price right before submitting the extrinsic so the
     * limit_price cushion reflects current chain state, not the (possibly
     * stale) value the picker rendered. Mirrors iOS
     * `SubtensorSubnetFetcher.fetchSubnetReserves(netuid:)`.
     */
    suspend fun fetchReserves(netuid: Int): SubtensorSubnetInfo? {
        val moduleHash = "SubtensorModule".toByteArray().xxHash128()
        val taoItemHash = "SubnetTAO".toByteArray().xxHash128()
        val alphaItemHash = "SubnetAlphaIn".toByteArray().xxHash128()

        val netuidLE = u16LE(netuid)
        val taoKey = "0x" + (moduleHash + taoItemHash + netuidLE).toHex()
        val alphaKey = "0x" + (moduleHash + alphaItemHash + netuidLE).toHex()

        val responses = sendBatch(
            listOf(
                rpcRequest(1, "state_getStorage", listOf(taoKey)),
                rpcRequest(2, "state_getStorage", listOf(alphaKey)),
            )
        )

        val taoReserve = decodeU64Le(responses[1])
        val alphaIn = decodeU64Le(responses[2])
        if (taoReserve.signum() <= 0) return null
        return SubtensorSubnetInfo(netuid = netuid, name = null, taoReserve = taoReserve, alphaInReserve = alphaIn)
    }

    private fun u16LE(value: Int): ByteArray = byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun decodeU64Le(hex: String?): BigInteger {
        if (hex.isNullOrBlank()) return BigInteger.ZERO
        val clean = hex.removePrefix("0x")
        if (clean.length < 16) return BigInteger.ZERO
        var result = BigInteger.ZERO
        for (i in 7 downTo 0) {
            val byteHex = clean.substring(i * 2, i * 2 + 2)
            val b = byteHex.toIntOrNull(16) ?: return BigInteger.ZERO
            result = result.shiftLeft(8).or(BigInteger.valueOf(b.toLong()))
        }
        return result
    }

    /**
     * SubnetIdentitiesV3 decodes as a struct whose first field is
     * `subnet_name: Vec<u8>`. Vec<u8> is encoded as a SCALE compact
     * length prefix followed by the raw UTF-8 bytes. We only need the
     * first field; downstream fields are ignored.
     */
    private fun decodeSubnetName(hex: String?): String? {
        if (hex.isNullOrBlank()) return null
        val clean = hex.removePrefix("0x")
        if (clean.length < 4) return null

        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: return null
        }
        if (bytes.size < 2) return null

        val first = bytes[0].toInt() and 0xFF
        val mode = first and 0b11
        val nameStart: Int
        val nameLength: Int
        when (mode) {
            0b00 -> {
                nameLength = first ushr 2
                nameStart = 1
            }
            0b01 -> {
                if (bytes.size < 2) return null
                val high = bytes[1].toInt() and 0xFF
                val raw = (first or (high shl 8))
                nameLength = raw ushr 2
                nameStart = 2
            }
            else -> return null
        }
        if (nameLength <= 0 || nameLength > 128) return null
        if (nameStart + nameLength > bytes.size) return null
        val nameBytes = bytes.copyOfRange(nameStart, nameStart + nameLength)
        return runCatching { String(nameBytes, Charsets.UTF_8) }.getOrNull()
    }
}
