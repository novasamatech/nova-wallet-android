package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetCommission
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class BittensorConfig(val commission: BittensorSubnetCommission, val subnetLogos: Map<BittensorNetuid.Subnet, String>)

fun interface BittensorConfigRepository {

    suspend fun config(): BittensorConfig

    suspend fun subnetLogos(): Map<BittensorNetuid.Subnet, String> = config().subnetLogos
}

/** Successful config is shared for this repository's lifetime; failed loads remain retryable. */
class RemoteBittensorConfigRepository(
    api: BittensorConfigApi,
    configUrl: String,
    private val logger: Logger
) : BittensorConfigRepository {

    private val cachedConfig = SingleValueCache {
        val remote = api.config(configUrl)
        mapBittensorConfig(remote)
    }

    override suspend fun config(): BittensorConfig {
        try {
            return withTimeoutOrNull(CONFIG_TIMEOUT_MILLIS) { cachedConfig() }
                ?: throw BittensorTimeoutException("Bittensor config request")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorConfig] Config unavailable; commission cannot be resolved")
            logger.log(e)
            throw e
        }
    }

    override suspend fun subnetLogos(): Map<BittensorNetuid.Subnet, String> {
        return try {
            config().subnetLogos
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorConfig] Optional subnet logos unavailable; retaining chain logos")
            emptyMap()
        }
    }
}

private const val CONFIG_TIMEOUT_MILLIS = 30_000L

// Decimal rate: 0.003 means 0.3%, not 0.003%.
private val DEFAULT_COMMISSION_FRACTION = BigDecimal("0.003")

internal fun fallbackBittensorConfig() = BittensorConfig(BittensorSubnetCommission(DEFAULT_COMMISSION_FRACTION), emptyMap())

internal fun mapBittensorConfig(remote: BittensorConfigRemote): BittensorConfig {
    val commission = mapBittensorCommission(remote.swapFee)
    val subnets = requireNotNull(remote.subnets) { "Bittensor subnet logos are missing" }
    val logos = mapBittensorSubnetLogos(subnets)
    return BittensorConfig(commission, logos)
}

private fun mapBittensorCommission(fee: BittensorSwapFeeRemote?): BittensorSubnetCommission {
    val fraction = when (fee) {
        is BittensorSwapFeeRemote.Value -> fee.fraction.inFraction
        BittensorSwapFeeRemote.Invalid, null -> DEFAULT_COMMISSION_FRACTION
    }

    return try {
        BittensorSubnetCommission(fraction)
    } catch (_: IllegalArgumentException) {
        // Only an invalid field in a successfully decoded config may use the established default.
        BittensorSubnetCommission(DEFAULT_COMMISSION_FRACTION)
    }
}

private fun mapBittensorSubnetLogos(subnets: List<BittensorConfigRemote.Subnet?>): Map<BittensorNetuid.Subnet, String> = buildMap {
    for (subnet in subnets.filterNotNull()) {
        val netuid = subnet.netuid ?: continue
        val subnetId = runCatching { BittensorNetuid.Subnet(netuid) }
            .getOrNull() ?: continue
        val logo = subnet.logo?.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: continue
        if (!containsKey(subnetId)) put(subnetId, logo.toString())
    }
}
