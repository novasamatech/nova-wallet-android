package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetLinks
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [fromChain] is false when the backend catalogue or the last good names stood in for a slow or failing node, so it
 * cannot cross-check the backend.
 */
data class BittensorSubnetCatalogue(val subnets: List<BittensorSubnet>, val fromChain: Boolean)

interface BittensorSubnetRepository {

    suspend fun subnets(): List<BittensorSubnet>

    fun observeSubnets(): Flow<List<BittensorSubnet>> = flowOf { subnets() }

    suspend fun catalogue(): BittensorSubnetCatalogue

    suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet

    suspend fun quickPickSubnets(): List<BittensorSubnet>
}

/** Composes subnet metadata, independent logo updates and live detail prices. */
class RealBittensorSubnetRepository(
    private val subnetSource: BittensorSubnetSource,
    private val configSource: BittensorConfigSource,
    private val swapSource: BittensorSwapSource
) : BittensorSubnetRepository {

    override suspend fun subnets(): List<BittensorSubnet> {
        val subnets = catalogue().subnets
        val logos = configSource.currentSubnetLogos()
        return subnets.map { it.copy(logoUrl = logos[it.id] ?: it.logoUrl) }
    }

    override fun observeSubnets(): Flow<List<BittensorSubnet>> = combine(
        subnetSource.observeCatalogue(),
        configSource.observeSubnetLogos()
    ) { catalogue, logos ->
        catalogue.subnets.map { it.copy(logoUrl = logos[it.id] ?: it.logoUrl) }
    }.distinctUntilChanged()

    override suspend fun catalogue(): BittensorSubnetCatalogue = subnetSource.catalogue()

    override suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        val catalogue = subnets()
        val subnet = catalogue.firstOrNull { it.id == subnetId }
            ?: throw NoSuchElementException("Subnet ${subnetId.value} is unavailable")
        val price = swapSource.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
        subnet.copy(priceInRao = price)
    } ?: throw BittensorTimeoutException("Subnet details")

    override suspend fun quickPickSubnets(): List<BittensorSubnet> = subnetSource.quickPickSubnets()
}

/**
 * The backend catalogue as [BittensorSubnet]s; STALE metadata is still listed. A STALE price never values
 * anything: [BittensorSubnet.priceInRao] stays null until a FRESH or live chain price is
 * available. The subnet picker still lists it from the backend rows whatever its freshness.
 */
internal fun mapBittensorBackendCatalogue(remote: BittensorSubnetCatalogueRemote): List<BittensorSubnet> {
    requireAvailableBittensorComponent(remote.meta?.components?.subnetMetadata, "Subnet metadata")
    requireAvailableBittensorComponent(remote.meta?.components?.alphaPrices, "Subnet prices")
    val prices = mapBittensorPortfolioPrices(remote)
    val subnets = requireNotNull(remote.items).mapNotNull { row ->
        val id = BittensorNetuid.from(requireNotNull(row.netuid)) as? BittensorNetuid.Subnet ?: return@mapNotNull null
        val registration = requireNotNull(row.networkRegisteredAt)
        require(registration.signum() >= 0) { "Invalid subnet registration" }
        BittensorSubnet(
            id = id,
            name = normalizeBittensorSubnetName(requireNotNull(row.name)),
            symbol = requireNotNull(row.symbol).trim(),
            logoUrl = null,
            priceInRao = prices[id],
            taoReserveInPlanks = row.taoReserve?.let(::bittensorBackendPlanks),
            registrationBlock = registration,
            alphaReserveInPlanks = row.alphaReserve?.let(::bittensorBackendPlanks),
            websiteUrl = BittensorSubnetLinks.website(row.subnetUrl),
            githubUrl = BittensorSubnetLinks.github(row.githubRepo)
        )
    }.distinctBy { it.id }.sortedBy { it.id.value }
    require(subnets.isNotEmpty()) { "Empty subnet catalogue" }
    return subnets
}

/** Every non-root netuid the catalogue lists; an unavailable component fails the read. */
internal fun mapBittensorListedSubnets(remote: BittensorSubnetCatalogueRemote): Set<BittensorNetuid.Subnet> {
    requireAvailableBittensorComponent(remote.meta?.components?.subnetMetadata, "Subnet metadata")
    requireAvailableBittensorComponent(remote.meta?.components?.alphaPrices, "Subnet prices")
    return requireNotNull(remote.items).mapNotNullTo(mutableSetOf()) { BittensorNetuid.from(requireNotNull(it.netuid)) as? BittensorNetuid.Subnet }
}

private fun bittensorBackendPlanks(value: String): BigInteger {
    val planks = value.toBigDecimal().movePointRight(BITTENSOR_DECIMALS).toBigIntegerExact()
    require(planks.signum() >= 0) { "Negative subnet reserve" }
    return planks
}

private const val BITTENSOR_DECIMALS = 9

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
        name = normalizeBittensorSubnetName(bindBittensorText(requireNotNull(value.get<Any?>("subnetName")) { "Subnet name field is missing" })),
        symbol = bindBittensorText(requireNotNull(value.get<Any?>("tokenSymbol")) { "Subnet symbol field is missing" }),
        logoUrl = logoUrl,
        priceInRao = bindBittensorSubnetPrice(value.get<Any?>("taoIn"), value.get<Any?>("alphaIn")),
        taoReserveInPlanks = value.get<Any?>("taoIn")
            ?.let(::bindBittensorU64),
        registrationBlock = value.get<Any?>("networkRegisteredAt")
            ?.let(::bindBittensorU64),
        alphaReserveInPlanks = value.get<Any?>("alphaIn")
            ?.let(::bindBittensorU64),
        websiteUrl = identity?.get<Any?>("subnetUrl")
            ?.let(::bindBittensorText)
            ?.let(BittensorSubnetLinks::website),
        githubUrl = identity?.get<Any?>("githubRepo")
            ?.let(::bindBittensorText)
            ?.let(BittensorSubnetLinks::github)
    )
}

// Subtensor reports "Unknown" for subnets without a name; such subnets are shown as "Subnet <netuid>" (as on iOS).
private const val UNNAMED_SUBNET_PLACEHOLDER = "Unknown"

/** Blank means unnamed: every display site then falls back to "Subnet <netuid>". */
internal fun normalizeBittensorSubnetName(name: String): String {
    val trimmed = name.trim()
    return if (trimmed.equals(UNNAMED_SUBNET_PLACEHOLDER, ignoreCase = true)) "" else trimmed
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
