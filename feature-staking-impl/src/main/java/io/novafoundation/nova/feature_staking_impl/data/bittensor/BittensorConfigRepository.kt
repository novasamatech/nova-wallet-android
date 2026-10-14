package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.JsonObject
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetCommission
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.http.GET
import retrofit2.http.Url
import java.math.BigDecimal

interface BittensorConfigApi {

    @GET
    suspend fun config(@Url url: String): JsonObject
}

data class BittensorConfig(val commission: BittensorSubnetCommission, val subnetLogos: Map<BittensorSubnetId, String>)

fun interface BittensorConfigRepository {

    suspend fun config(): BittensorConfig
}

class RemoteBittensorConfigRepository(
    api: BittensorConfigApi,
    configUrl: String,
    private val logger: Logger
) : BittensorConfigRepository {

    private val cachedConfig = SingleValueCache { bindBittensorConfig(api.config(configUrl)) }

    override suspend fun config(): BittensorConfig {
        try {
            withTimeoutOrNull(15_000L) { cachedConfig() }?.let { return it }
            logger.log("[BittensorConfig] Config request timed out; using fallback fee and chain logos")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorConfig] Config unavailable; using fallback fee and chain logos")
            logger.log(e)
        }
        return fallbackBittensorConfig()
    }
}

internal fun fallbackBittensorConfig() = BittensorConfig(BittensorSubnetCommission(BigDecimal("0.003")), emptyMap())

internal fun bindBittensorConfig(json: JsonObject): BittensorConfig {
    val fraction = json["swapFee"]?.takeIf { it.isJsonPrimitive }?.asString?.toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO && it < BigDecimal.ONE }
    val commission = fraction?.let(::BittensorSubnetCommission) ?: fallbackBittensorConfig().commission
    val entries = requireNotNull(json["subnets"]?.takeIf { it.isJsonArray }) { "Bittensor subnet logos are missing" }.asJsonArray
    val logos = buildMap {
        for (entry in entries) {
            val subnet = entry.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val netuid = subnet["netuid"]?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()?.takeIf { it in 1..65535 } ?: continue
            val logo = subnet["logo"]?.takeIf { it.isJsonPrimitive }?.asString?.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: continue
            val subnetId = BittensorSubnetId(netuid)
            if (!containsKey(subnetId)) put(subnetId, logo.toString())
        }
    }
    return BittensorConfig(commission, logos)
}
