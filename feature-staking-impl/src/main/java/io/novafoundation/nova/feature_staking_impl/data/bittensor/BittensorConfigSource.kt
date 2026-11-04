package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetCommission
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class BittensorConfig(val commission: BittensorSubnetCommission, val subnetLogos: Map<BittensorNetuid.Subnet, String>)

fun interface BittensorConfigSource {

    suspend fun config(): BittensorConfig

    suspend fun subnetLogos(): Map<BittensorNetuid.Subnet, String> = config().subnetLogos

    fun currentSubnetLogos(): Map<BittensorNetuid.Subnet, String> = emptyMap()

    fun observeSubnetLogos(): Flow<Map<BittensorNetuid.Subnet, String>> =
        flowOf { subnetLogos() }.onStart { emit(currentSubnetLogos()) }
}

class RemoteBittensorConfigSource(
    private val api: BittensorConfigApi,
    private val configUrl: String,
    private val logger: Logger,
    scope: CoroutineScope,
    foregroundRefreshes: Flow<Unit>
) : BittensorConfigSource {

    private val cachedConfig = MutableStateFlow<BittensorConfig?>(null)

    private val mutex = Mutex()

    private val logos = channelFlow {
        launch {
            merge(flowOf(Unit), foregroundRefreshes).collectLatest {
                while (cachedConfig.value == null) {
                    try {
                        config()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // config() reports the error; stay subscribed and retry at a bounded rate.
                        delay(LOGO_RETRY_INTERVAL_MILLIS)
                    }
                }
            }
        }
        cachedConfig.map { it?.subnetLogos.orEmpty() }.distinctUntilChanged().collect { send(it) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), replay = 1)

    override suspend fun config(): BittensorConfig {
        try {
            return withTimeoutOrNull(CONFIG_TIMEOUT_MILLIS) {
                mutex.withLock {
                    cachedConfig.value ?: mapBittensorConfig(api.config(configUrl)).also { cachedConfig.value = it }
                }
            }
                ?: throw BittensorTimeoutException("Bittensor config request")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorConfig] Config unavailable; commission cannot be resolved")
            logger.log(e)
            throw e
        }
    }

    override fun currentSubnetLogos(): Map<BittensorNetuid.Subnet, String> = cachedConfig.value?.subnetLogos.orEmpty()

    override fun observeSubnetLogos(): Flow<Map<BittensorNetuid.Subnet, String>> = logos

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

private const val LOGO_RETRY_INTERVAL_MILLIS = 30_000L

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
