package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetCommission
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class BittensorConfig(val commission: BittensorSubnetCommission, val subnetLogos: Map<BittensorNetuid.Subnet, String>)

fun interface BittensorConfigRepository {

    suspend fun config(): BittensorConfig

    suspend fun subnetLogos(): Map<BittensorNetuid.Subnet, String> = config().subnetLogos
}

class RemoteBittensorConfigRepository(
    api: BittensorConfigApi,
    configUrl: String,
    private val logger: Logger
) : BittensorConfigRepository {

    private val cachedConfig = SingleValueCache { mapBittensorConfig(api.config(configUrl)) }

    override suspend fun config(): BittensorConfig {
        try {
            return withTimeoutOrNull(15_000L) { cachedConfig() }
                ?: throw IOException("Bittensor config request timed out")
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

internal fun fallbackBittensorConfig() = BittensorConfig(BittensorSubnetCommission(BigDecimal("0.003")), emptyMap())

internal fun mapBittensorConfig(remote: BittensorConfigRemote): BittensorConfig {
    val fraction = remote.swapFee?.takeIf { it.isNonNegative && it < BigDecimal.ONE }
    val commission = fraction?.let(::BittensorSubnetCommission) ?: fallbackBittensorConfig().commission
    val subnets = requireNotNull(remote.subnets) { "Bittensor subnet logos are missing" }
    val logos = buildMap {
        for (subnet in subnets.filterNotNull()) {
            val netuid = subnet.netuid ?: continue
            val subnetId = runCatching { BittensorNetuid.Subnet(netuid) }
                .getOrNull() ?: continue
            val logo = subnet.logo?.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: continue
            if (!containsKey(subnetId)) put(subnetId, logo.toString())
        }
    }
    return BittensorConfig(commission, logos)
}
