package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetLinks
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [fromChain] is false when the backend catalogue or the last good names stood in for a slow or failing node, so it
 * cannot cross-check the backend.
 */
class BittensorSubnetCatalogue(val subnets: List<BittensorSubnet>, val fromChain: Boolean)

interface BittensorSubnetRepository {

    suspend fun subnets(): List<BittensorSubnet>

    suspend fun catalogue(): BittensorSubnetCatalogue

    suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet

    suspend fun quickPickSubnets(): List<BittensorSubnet>
}

/**
 * Subnet names and symbols come from the chain (`get_all_dynamic_info`, a heavy call) or, when the node is slow or
 * failing, from the backend `/subnets` catalogue - the source iOS names subnets from. When both fail, the last good
 * catalogue stands in with its metadata only, so a failing node never blanks subnet names; prices and reserves are never
 * served from that last good catalogue, and the next read asks both sources again.
 */
class RealBittensorSubnetRepository(
    private val runtimeCalls: MultiChainRuntimeCallsApi,
    private val configRepository: BittensorConfigRepository,
    private val swapSource: BittensorSwapSource,
    private val catalogueSource: BittensorSubnetCatalogueRemoteSource,
    private val chainPatienceMillis: Long = CHAIN_CATALOGUE_PATIENCE_MILLIS,
    private val nanoTime: () -> Long = System::nanoTime
) : BittensorSubnetRepository {

    private val mutex = Mutex()

    private var cachedCatalogue: BittensorSubnetCatalogue? = null

    private var lastGoodCatalogue: BittensorSubnetCatalogue? = null

    private var catalogueLoadedAtNanos = 0L

    override suspend fun subnets(): List<BittensorSubnet> = catalogue().subnets

    override suspend fun catalogue(): BittensorSubnetCatalogue = mutex.withLock {
        val cachedCatalogue = cachedCatalogue
        if (cachedCatalogue != null && nanoTime() - catalogueLoadedAtNanos < CATALOGUE_VALIDITY.inWholeNanoseconds) {
            return@withLock cachedCatalogue
        }
        val catalogue = try {
            loadCatalogue()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock lastGoodCatalogue ?: throw e
        }
        this.cachedCatalogue = catalogue
        lastGoodCatalogue = catalogue.namesOnly()
        catalogueLoadedAtNanos = nanoTime()
        catalogue
    }

    // Prices and reserves move every block: what outlives a failed load names subnets but never values anything.
    private fun BittensorSubnetCatalogue.namesOnly() = BittensorSubnetCatalogue(
        subnets.map { it.copy(priceInRao = null, taoReserveInPlanks = null, alphaReserveInPlanks = null) },
        fromChain = false
    )

    override suspend fun subnet(subnetId: BittensorNetuid.Subnet): BittensorSubnet = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        val catalogue = subnets()
        val subnet = catalogue.firstOrNull { it.id == subnetId }
            ?: throw NoSuchElementException("Subnet ${subnetId.value} is unavailable")
        val price = swapSource.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
        subnet.copy(priceInRao = price)
    } ?: throw BittensorTimeoutException("Subnet details")

    override suspend fun quickPickSubnets(): List<BittensorSubnet> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        loadDynamicSubnets(requireComplete = true)
    } ?: throw BittensorTimeoutException("Subnet quick-pick catalogue")

    private suspend fun loadCatalogue(): BittensorSubnetCatalogue = coroutineScope {
        val subnetLogos = async { configRepository.subnetLogos() }
        var catalogue: BittensorSubnetCatalogue? = null
        // Logos only decorate the catalogue: a config read slower than the catalogue budget costs the logos, not the catalogue.
        val logos = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
            catalogue = resolveCatalogue()
            subnetLogos.await()
        }
        subnetLogos.cancel()
        val resolved = catalogue ?: throw BittensorTimeoutException("Subnet catalogue")
        BittensorSubnetCatalogue(resolved.subnets.map { it.copy(logoUrl = logos?.get(it.id) ?: it.logoUrl) }, resolved.fromChain)
    }

    private suspend fun resolveCatalogue(): BittensorSubnetCatalogue = coroutineScope {
        val chain = async { catching { loadDynamicSubnets() } }
        // Prefer the chain while it answers promptly. Only a slow or failing node costs an attested backend request,
        // so a healthy node never spends the Profile-3 rate budget.
        val promptChain = withTimeoutOrNull(chainPatienceMillis) { chain.await() }?.getOrNull()
        val catalogue = if (promptChain != null) {
            BittensorSubnetCatalogue(promptChain, fromChain = true)
        } else {
            val backend = async { catching { loadBackendCatalogue() } }
            firstSuccess(chain, backend).also { backend.cancel() }
        }
        chain.cancel()
        catalogue
    }

    /** Whichever source answers first with a catalogue; the chain's failure is reported when both fail. */
    private suspend fun firstSuccess(
        chain: Deferred<Result<List<BittensorSubnet>>>,
        backend: Deferred<Result<List<BittensorSubnet>>>
    ): BittensorSubnetCatalogue {
        val first = select {
            chain.onAwait { result -> result.getOrNull()?.let { BittensorSubnetCatalogue(it, fromChain = true) } }
            backend.onAwait { result -> result.getOrNull()?.let { BittensorSubnetCatalogue(it, fromChain = false) } }
        }
        if (first != null) return first
        backend.await().getOrNull()?.let { return BittensorSubnetCatalogue(it, fromChain = false) }
        return BittensorSubnetCatalogue(chain.await().getOrThrow(), fromChain = true)
    }

    private suspend fun loadBackendCatalogue(): List<BittensorSubnet> = mapBittensorBackendCatalogue(catalogueSource.catalogue())

    private suspend fun <T> catching(load: suspend () -> T): Result<T> = try {
        Result.success(load())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun loadDynamicSubnets(requireComplete: Boolean = false): List<BittensorSubnet> = runtimeCalls.forChain(ChainGeneses.BITTENSOR)
        .call(
            section = "SubnetInfoRuntimeApi",
            method = "get_all_dynamic_info",
            arguments = emptyMap(),
            returnBinding = { decoded -> bindBittensorSubnets(decoded, requireComplete) }
        )

    private companion object {

        val CATALOGUE_VALIDITY = 30.seconds

        const val CHAIN_CATALOGUE_PATIENCE_MILLIS = 4_000L
    }
}

/**
 * The backend catalogue as [BittensorSubnet]s; STALE metadata is still listed, as on iOS. A STALE price never values
 * anything: [BittensorSubnet.priceInRao] stays null (iOS `freshTaoPerAlpha`) until a FRESH or live chain price is
 * available. The subnet picker still lists it from the backend rows, as iOS lists `taoPerAlpha` whatever its freshness.
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
