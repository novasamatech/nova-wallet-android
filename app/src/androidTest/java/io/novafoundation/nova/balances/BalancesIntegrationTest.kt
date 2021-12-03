package io.novafoundation.nova.balances

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@RunWith(AndroidJUnit4::class)
class BalancesIntegrationTest {

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val chainRegistry = runtimeApi.chainRegistry()
    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    private val remoteStorage = runtimeApi.remoteStorageSource()

    @Test
    fun testBalancesLoading() = runBlocking(Dispatchers.Default) {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)

        val chains = chainRegistry.currentChains.first()

        val errorMessages = chains.map { chain -> testBalancesInChainAsync(chain) }
            .awaitAll()
            .filterNotNull()
            .map { (error, chain) ->
                "${chain.name}: ${error.message} (${(error.javaClass.simpleName)})"
            }

        // TODO maybe pass exceptions to bot?
        if (errorMessages.isNotEmpty()) {
            val overallMessage = errorMessages.joinToString(separator = "\n")

            throw Exception("Failed to fetch balances in all networks: \n ${overallMessage}}")
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun CoroutineScope.testBalancesInChainAsync(chain: Chain): Deferred<Pair<Throwable, Chain>?> {
        return async {
            val exception = runCatching {
                withTimeout(10.seconds) {
                    remoteStorage.query(
                        chainId = chain.id,
                        keyBuilder = { it.metadata.system().storage("Account").storageKey(it, chain.sampleAccountId()) },
                        binding = { scale, runtime -> scale?.let { bindAccountInfo(scale, runtime) } }
                    )
                }
            }.exceptionOrNull()

            println("Done for ${chain.name}: ${if (exception == null) "Success" else "Failure"}")
            Log.d(this@BalancesIntegrationTest.LOG_TAG, "Done for ${chain.name}: ${if (exception == null) "Success" else "${exception.message}"}")

            exception?.let { it to chain }
        }
    }

    private fun Chain.sampleAccountId() = if (isEthereumBased) {
        ByteArray(20) { 1 }
    } else {
        ByteArray(32) { 1 }
    }
}
