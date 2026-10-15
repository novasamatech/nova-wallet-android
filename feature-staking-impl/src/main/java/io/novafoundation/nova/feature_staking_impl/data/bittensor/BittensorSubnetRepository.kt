package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

interface BittensorSubnetRepository {

    suspend fun subnets(): List<BittensorSubnet>

    suspend fun subnet(subnetId: BittensorSubnetId): BittensorSubnet
}

class RealBittensorSubnetRepository(
    private val runtimeCalls: MultiChainRuntimeCallsApi,
    private val configRepository: BittensorConfigRepository,
    private val swapSource: BittensorSwapSource
) : BittensorSubnetRepository {

    private val mutex = Mutex()

    private var cachedCatalogue: List<BittensorSubnet>? = null

    private var catalogueLoadedAtNanos = 0L

    override suspend fun subnets(): List<BittensorSubnet> = mutex.withLock {
        val cachedCatalogue = cachedCatalogue
        if (cachedCatalogue != null && System.nanoTime() - catalogueLoadedAtNanos < CATALOGUE_VALIDITY.inWholeNanoseconds) {
            return@withLock cachedCatalogue
        }
        val catalogue = loadCatalogue()
        this.cachedCatalogue = catalogue
        catalogueLoadedAtNanos = System.nanoTime()
        catalogue
    }

    override suspend fun subnet(subnetId: BittensorSubnetId): BittensorSubnet = withTimeout(20_000L) {
        val subnet = subnets().firstOrNull { it.id == subnetId } ?: throw NoSuchElementException("Subnet ${subnetId.value} is unavailable")
        val price = swapSource.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
        subnet.copy(priceInRao = price)
    }

    private suspend fun loadCatalogue(): List<BittensorSubnet> = withTimeout(20_000L) {
        coroutineScope {
            val config = async { configRepository.config() }
            val subnets = runtimeCalls.forChain(ChainGeneses.BITTENSOR).call(
                section = "SubnetInfoRuntimeApi",
                method = "get_all_dynamic_info",
                arguments = emptyMap(),
                returnBinding = ::bindBittensorSubnets
            )
            val logos = config.await().subnetLogos
            subnets.map { it.copy(logoUrl = logos[it.id] ?: it.logoUrl) }
        }
    }

    private companion object {

        val CATALOGUE_VALIDITY = 30.seconds
    }
}

internal fun bindBittensorSubnets(decoded: Any?): List<BittensorSubnet> = bindList(decoded) { item ->
    if (item == null) return@bindList null
    val value = item.castToStruct()
    val id = bindBittensorU16(value["netuid"])
    if (id == 0) return@bindList null
    // The SDK normalizes composite fields from metadata to camelCase.
    val identity = value.get<Any?>("subnetIdentity")?.castToStruct()
    BittensorSubnet(
        id = BittensorSubnetId(id),
        name = bindBittensorText(requireNotNull(value.get<Any?>("subnetName")) { "Subnet name field is missing" }),
        symbol = bindBittensorText(requireNotNull(value.get<Any?>("tokenSymbol")) { "Subnet symbol field is missing" }),
        logoUrl = identity?.get<Any?>("logoUrl")?.let(::bindBittensorText)?.takeIf { it.startsWith("https://") },
        priceInRao = bindBittensorSubnetPrice(value.get<Any?>("taoIn"), value.get<Any?>("alphaIn")),
        taoReserveInPlanks = value.get<Any?>("taoIn")?.let(::bindBittensorU64)
    )
}.filterNotNull().sortedBy { it.id.value }.also { subnets ->
    require(subnets.map { it.id }.distinct().size == subnets.size) { "Duplicate subnet ids" }
}

/** SCALE text decodes as bytes or a Vec<u8>, not a Kotlin String. */
internal fun bindBittensorText(decoded: Any?): String {
    val bytes = when (decoded) {
        is ByteArray -> decoded
        else -> bindList(decoded) { bindBittensorU8(it).toByte() }.toByteArray()
    }
    return bytes.decodeToString(throwOnInvalidSequence = true).trim()
}

internal fun bindBittensorSubnetPrice(tao: Any?, alpha: Any?): BigInteger? {
    if (tao == null || alpha == null) return null
    val taoReserve = bindBittensorU64(tao)
    val alphaReserve = bindBittensorU64(alpha)
    if (taoReserve.isZero || alphaReserve.isZero) return null
    return (taoReserve * BigInteger.TEN.pow(9) / alphaReserve).takeIf { it.isPositive() }
}
