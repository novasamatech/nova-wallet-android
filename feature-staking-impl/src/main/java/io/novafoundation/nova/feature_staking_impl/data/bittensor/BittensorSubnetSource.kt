package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Owns runtime catalogue loading, backend fallback and reuse of successful metadata. */
class BittensorSubnetSource(
    private val runtimeCalls: MultiChainRuntimeCallsApi,
    private val catalogueSource: BittensorSubnetCatalogueSource,
    private val chainPatienceMillis: Long = CHAIN_CATALOGUE_PATIENCE_MILLIS,
    private val nanoTime: () -> Long = System::nanoTime
) {

    private val mutex = Mutex()

    private var cachedCatalogue: BittensorSubnetCatalogue? = null

    private var lastGoodCatalogue: BittensorSubnetCatalogue? = null

    private var catalogueLoadedAtNanos = 0L

    suspend fun catalogue(): BittensorSubnetCatalogue = mutex.withLock {
        val cachedCatalogue = cachedCatalogue
        if (cachedCatalogue != null && cachedCatalogue.fromChain && nanoTime() - catalogueLoadedAtNanos < CATALOGUE_VALIDITY.inWholeNanoseconds) {
            return@withLock cachedCatalogue
        }
        val backend = catalogueSource.availableSubnets()
        if (backend != null) {
            return@withLock BittensorSubnetCatalogue(mapBittensorBackendCatalogue(backend), fromChain = false)
        }
        val catalogue = try {
            resolveCatalogue()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock lastGoodCatalogue ?: throw e
        }
        this.cachedCatalogue = catalogue
        if (catalogue.fromChain) lastGoodCatalogue = catalogue.namesOnly()
        catalogueLoadedAtNanos = nanoTime()
        catalogue
    }

    // Prices and reserves move every block: what outlives a failed load names subnets but never values anything.
    private fun BittensorSubnetCatalogue.namesOnly() = BittensorSubnetCatalogue(
        subnets.map { it.copy(priceInRao = null, taoReserveInPlanks = null, alphaReserveInPlanks = null) },
        fromChain = false
    )

    suspend fun quickPickSubnets(): List<BittensorSubnet> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        loadDynamicSubnets(requireComplete = true)
    } ?: throw BittensorTimeoutException("Subnet quick-pick catalogue")

    private suspend fun resolveCatalogue(): BittensorSubnetCatalogue = coroutineScope {
        val chain = async {
            runCatching { loadDynamicSubnets() }
                .onFailure { if (it is CancellationException) throw it }
        }
        // Prefer the chain while it answers promptly. Only a slow or failing node costs an attested backend request,
        // so a healthy node never spends the Profile-3 rate budget.
        val promptChain = withTimeoutOrNull(chainPatienceMillis) { chain.await() }?.getOrNull()
        val catalogue = if (promptChain != null) {
            BittensorSubnetCatalogue(promptChain, fromChain = true)
        } else {
            val backend = async {
                runCatching { loadBackendCatalogue() }
                    .onFailure { if (it is CancellationException) throw it }
            }
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

    private suspend fun loadBackendCatalogue(): List<BittensorSubnet> = mapBittensorBackendCatalogue(catalogueSource.subnets())

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
