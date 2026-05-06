package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorValidator
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigInteger

/**
 * Merges Bittensor validator identity (from the GitHub delegates registry)
 * with numeric data (stake / commission / APR / nominator counts) from a
 * pluggable [SubtensorValidatorDataSource] implementation.
 *
 * Mirrors iOS `SubtensorValidatorProvider`. When the numeric source returns
 * an empty list (release builds without a TaoStats key, or transient
 * failure) the provider falls back to identity-only rows so the picker
 * still has something to show.
 *
 * v1 only ever asks for root-netuid validators, but the parameter is
 * plumbed through unchanged to keep subnet variants drop-in.
 */
class SubtensorValidatorProvider(
    private val delegatesClient: BittensorDelegatesClient,
    private val dataSource: SubtensorValidatorDataSource,
    private val identityAddressPrefix: Short,
) {

    suspend fun fetchValidators(netuid: Int): List<SubtensorValidator> = coroutineScope {
        val delegatesDeferred: Deferred<Map<String, BittensorDelegateMetadata>> = async {
            runCatching { delegatesClient.fetchDelegates() }
                .getOrElse { delegatesClient.cachedDelegates() }
        }
        val numericDeferred: Deferred<List<SubtensorValidator>> = async {
            runCatching { dataSource.fetchValidators(netuid) }.getOrDefault(emptyList())
        }

        val delegates = delegatesDeferred.await()
        val numeric = numericDeferred.await()

        val merged: List<SubtensorValidator> = if (numeric.isNotEmpty()) {
            numeric.map { row ->
                val identityKey: String? = runCatching { row.hotkey.toAddress(identityAddressPrefix) }.getOrNull()
                val metadata: BittensorDelegateMetadata? = identityKey?.let { delegates[it] }
                row.copy(
                    identity = row.identity ?: metadata?.name,
                    url = row.url ?: metadata?.url,
                    description = row.description ?: metadata?.description,
                )
            }
        } else {
            // Identity-only fallback. Mirrors iOS — the "hotkey" produced
            // here is a deterministic derivation from the SS58 string so
            // the rest of the UI can still keyed-update on it. NOT
            // cryptographically meaningful.
            delegates.map { (ss58, metadata) ->
                val hotkey = runCatching { ss58.toAccountId() }
                    .getOrElse { placeholderAccountId(ss58) }
                SubtensorValidator(
                    hotkey = hotkey,
                    netuid = netuid,
                    identity = metadata.name,
                    url = metadata.url,
                    description = metadata.description,
                    totalStake = BigInteger.ZERO,
                    ownStake = BigInteger.ZERO,
                    nominatorCount = null,
                    commission = null,
                    apr = null,
                )
            }
        }

        // Sort: total stake desc, ties broken by identity ascending. iOS
        // does the same — keeps the picker stable across refreshes.
        merged.sortedWith(
            compareByDescending<SubtensorValidator> { it.totalStake }
                .thenBy { it.identity ?: "" }
        )
    }

    private fun placeholderAccountId(ss58: String): ByteArray {
        val out = ByteArray(32)
        for ((i, b) in ss58.toByteArray().withIndex()) {
            out[i % 32] = (out[i % 32].toInt() xor b.toInt()).toByte()
        }
        return out
    }

    private fun SubtensorValidator.copy(
        identity: String? = this.identity,
        url: String? = this.url,
        description: String? = this.description,
    ): SubtensorValidator = SubtensorValidator(
        hotkey = hotkey,
        netuid = netuid,
        identity = identity,
        url = url,
        description = description,
        totalStake = totalStake,
        ownStake = ownStake,
        nominatorCount = nominatorCount,
        commission = commission,
        apr = apr,
    )
}
