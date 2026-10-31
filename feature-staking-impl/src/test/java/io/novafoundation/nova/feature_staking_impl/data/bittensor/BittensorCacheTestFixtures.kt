package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.BittensorCacheDao
import io.novafoundation.nova.core_db.model.BittensorCacheLocal
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.mockito.Mockito.mock

class InMemoryBittensorCacheDao : BittensorCacheDao {

    val rows = linkedMapOf<String, BittensorCacheLocal>()

    override suspend fun get(key: String) = rows[key]

    override suspend fun put(entry: BittensorCacheLocal) { rows[entry.key] = entry }

    override suspend fun generations(namespace: String) = rows.values.filter { it.namespace == namespace && it.generationAsOf != null }

    override suspend fun keepNamespace(namespace: String) { rows.entries.removeAll { it.value.namespace != namespace } }

    override suspend fun dropGenerations(namespace: String) { rows.entries.removeAll { it.value.namespace == namespace && it.value.generationAsOf != null } }

    override suspend fun invalidate(namespace: String, account: String, resources: List<String>) {
        rows.entries.removeAll { it.value.namespace == namespace && it.value.account == account && it.value.resource in resources }
    }

    override suspend fun trim(limit: Int) {
        rows.values.filter { it.resource != "__generation" }.sortedByDescending { it.fetchedAt }.drop(limit).forEach { rows.remove(it.key) }
    }
}

fun createBittensorTestCache(
    urls: InfrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() },
    nanoTime: () -> Long = System::nanoTime,
    nowMillis: () -> Long = System::currentTimeMillis,
    dao: BittensorCacheDao = InMemoryBittensorCacheDao(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
): BittensorResponseCache {
    val gson = Gson()
    val foreground = emptyFlow<Boolean>()
    val logger = mock(Logger::class.java)
    val flightScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[kotlinx.coroutines.Job]))
    return BittensorResponseCache(dao, gson, urls, flightScope, foreground, logger, nowMillis, nanoTime)
}

// Existing contract suites use named fixture factories while production receives the shared feature cache through DI.
fun BittensorValidatorBackend(
    api: BittensorValidatorsApi,
    infrastructureUrls: InfrastructureUrls,
    nanoTime: () -> Long = System::nanoTime
): BittensorValidatorBackend {
    val cache = createBittensorTestCache(infrastructureUrls, nanoTime)
    return BittensorValidatorBackend(api, cache, readExpiredCache = false)
}

fun BittensorSubnetCatalogueRepository(
    api: BittensorSubnetCatalogueApi,
    infrastructureUrls: InfrastructureUrls,
    nanoTime: () -> Long = System::nanoTime
): BittensorSubnetCatalogueRepository {
    val cache = createBittensorTestCache(infrastructureUrls, nanoTime)
    return BittensorSubnetCatalogueRepository(api, cache)
}
