package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

interface BittensorSubnetRepository {

    suspend fun subnets(): List<BittensorSubnet>

    suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet

    suspend fun quickPickSubnets(): List<BittensorSubnet>
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

    override suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        val catalogue = subnets()
        val subnet = catalogue.firstOrNull { it.id == subnetId }
            ?: throw NoSuchElementException("Subnet ${subnetId.value} is unavailable")
        val price = swapSource.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
        subnet.copy(priceInRao = price)
    } ?: error("Subnet details timed out")

    override suspend fun quickPickSubnets(): List<BittensorSubnet> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        loadDynamicSubnets(requireComplete = true)
    } ?: error("Subnet quick-pick catalogue timed out")

    private suspend fun loadCatalogue(): List<BittensorSubnet> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        coroutineScope {
            val subnetLogos = async { configRepository.subnetLogos() }
            val subnets = loadDynamicSubnets()
            val logos = subnetLogos.await()
            subnets.map { it.copy(logoUrl = logos[it.id] ?: it.logoUrl) }
        }
    } ?: error("Subnet catalogue timed out")

    private suspend fun loadDynamicSubnets(requireComplete: Boolean = false): List<BittensorSubnet> = runtimeCalls.forChain(ChainGeneses.BITTENSOR)
        .call(
            section = "SubnetInfoRuntimeApi",
            method = "get_all_dynamic_info",
            arguments = emptyMap(),
            returnBinding = { decoded -> bindBittensorSubnets(decoded, requireComplete) }
        )

    private companion object {

        val CATALOGUE_VALIDITY = 30.seconds
    }
}

internal fun bindBittensorSubnets(decoded: Any?, requireComplete: Boolean = false): List<BittensorSubnet> {
    if (requireComplete) {
        val rows = bindList(decoded) { it }
        require(rows.none { it == null }) { "Subnet catalogue is incomplete" }
    }

    val subnets = bindList(decoded, ::bindBittensorSubnet)
        .filterNotNull().sortedBy { it.id.value }

    val subnetIds = subnets.map { it.id }
        .distinct()
    require(subnetIds.size == subnets.size) { "Duplicate subnet ids" }
    return subnets
}

private fun bindBittensorSubnet(decoded: Any?): BittensorSubnet? {
    if (decoded == null) return null

    val value = decoded.castToStruct()
    val netuid = BittensorNetuid.from(bindBittensorU16(value["netuid"]))
    if (netuid !is BittensorNetuid.Subnet) return null
    // The SDK normalizes composite fields from metadata to camelCase.

    val identity = value.get<Any?>("subnetIdentity")
        ?.castToStruct()

    val logoUrl = identity?.get<Any?>("logoUrl")
        ?.let(::bindBittensorText)
        ?.takeIf { it.startsWith("https://") }
    return BittensorSubnet(
        id = netuid,
        name = bindBittensorText(requireNotNull(value.get<Any?>("subnetName")) { "Subnet name field is missing" }),
        symbol = bindBittensorText(requireNotNull(value.get<Any?>("tokenSymbol")) { "Subnet symbol field is missing" }),
        logoUrl = logoUrl,
        priceInRao = bindBittensorSubnetPrice(value.get<Any?>("taoIn"), value.get<Any?>("alphaIn")),
        taoReserveInPlanks = value.get<Any?>("taoIn")
            ?.let(::bindBittensorU64),
        registrationBlock = value.get<Any?>("networkRegisteredAt")
            ?.let(::bindBittensorU64),
        alphaReserveInPlanks = value.get<Any?>("alphaIn")
            ?.let(::bindBittensorU64)
    )
}

/** SCALE text decodes as bytes or a Vec<u8>, not a Kotlin String. */
internal fun bindBittensorText(decoded: Any?): String {
    val bytes = when (decoded) {
        is ByteArray -> decoded
        else -> bindList(decoded) {
            bindBittensorU8(it).toByte()
        }
            .toByteArray()
    }
    return bytes.decodeToString(throwOnInvalidSequence = true)
        .trim()
}

internal fun bindBittensorSubnetPrice(tao: Any?, alpha: Any?): BigInteger? {
    if (tao == null || alpha == null) return null

    val taoReserve = bindBittensorU64(tao)
    val alphaReserve = bindBittensorU64(alpha)
    if (taoReserve.isZero || alphaReserve.isZero) return null
    return (taoReserve * BigInteger.TEN.pow(9) / alphaReserve).takeIf { it.isPositive() }
}
