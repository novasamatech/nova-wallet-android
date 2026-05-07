package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorValidator
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigInteger

/**
 * [TEMP-TAOSTATS] Phase B numeric data source backed by TaoStats v1 REST.
 *
 * Endpoint: `https://api.taostats.io/api/validator/latest/v1?limit=200`
 * Auth: raw API key in the `Authorization` header (no `Bearer` prefix).
 *
 * Mirrors iOS `TaoStatsValidatorDataSource` — same endpoint, same field
 * mapping, same APR-fallback rule (30d average if > 0, else daily). The
 * hotkey we emit is the decoded AccountId so the picker's selection round
 * trip can encode it back to ss58 with whatever prefix the chain uses.
 *
 * When Nova's Bittensor indexer ships, this whole class is deleted and
 * the DI binding swaps to a Nova-backed implementation — no consumer
 * code change.
 */
class TaoStatsValidatorDataSource(
    private val apiKey: String,
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) : SubtensorValidatorDataSource {

    override suspend fun fetchValidators(netuid: Int): List<SubtensorValidator> {
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", apiKey)
            .header("Accept", "application/json")
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val text = response.body?.string() ?: return emptyList()
            parse(text, netuid)
        }
    }

    fun parse(json: String, netuid: Int): List<SubtensorValidator> {
        val parsed = runCatching { gson.fromJson(json, TaoStatsResponse::class.java) }.getOrNull() ?: return emptyList()
        val rows = parsed.data ?: return emptyList()
        return rows.mapNotNull { row -> row.toValidator(netuid) }
    }

    private fun TaoStatsValidatorRow.toValidator(netuid: Int): SubtensorValidator? {
        val ss58 = hotkey?.ss58 ?: return null
        val accountId = runCatching { ss58.toAccountId() }.getOrNull() ?: return null

        val totalStake = stake?.let { runCatching { BigInteger(it) }.getOrNull() } ?: BigInteger.ZERO
        val ownStake = validatorStake?.let { runCatching { BigInteger(it) }.getOrNull() } ?: BigInteger.ZERO
        val commission = take?.toDoubleOrNull()
        // Prefer 30-day average when positive, else fall back to daily APR.
        // Mirrors iOS exactly so picker rows match across platforms.
        val apr = apr30DayAverage?.toDoubleOrNull()?.takeIf { it > 0 }
            ?: apr?.toDoubleOrNull()
        val resolvedName = name?.trim()?.takeIf { it.isNotEmpty() }

        return SubtensorValidator(
            hotkey = accountId,
            netuid = netuid,
            identity = resolvedName,
            url = null,
            description = null,
            totalStake = totalStake,
            ownStake = ownStake,
            nominatorCount = nominators?.takeIf { it >= 0 },
            commission = commission,
            apr = apr,
        )
    }

    private companion object {
        const val ENDPOINT = "https://api.taostats.io/api/validator/latest/v1?limit=200"
    }

    private data class TaoStatsResponse(
        @SerializedName("data") val data: List<TaoStatsValidatorRow>?,
    )

    private data class TaoStatsValidatorRow(
        @SerializedName("hotkey") val hotkey: TaoStatsHotkey?,
        @SerializedName("name") val name: String?,
        @SerializedName("stake") val stake: String?,
        @SerializedName("validator_stake") val validatorStake: String?,
        @SerializedName("take") val take: String?,
        @SerializedName("apr") val apr: String?,
        @SerializedName("apr_30_day_average") val apr30DayAverage: String?,
        @SerializedName("nominators") val nominators: Int?,
    )

    private data class TaoStatsHotkey(
        @SerializedName("ss58") val ss58: String?,
    )
}
