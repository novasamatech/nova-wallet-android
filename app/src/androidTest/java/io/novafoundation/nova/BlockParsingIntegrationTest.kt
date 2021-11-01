package io.novafoundation.nova

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.data.network.runtime.binding.bindEventRecords
import io.novafoundation.nova.common.data.network.runtime.binding.storageReturnType
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockParsingIntegrationTest {

    private val chainGenesis = "4ac80c99289841dd946ef92765bf659a307d39189b3ce374a92b5f0415ee17a1" // Calamari

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val chainRegistry = runtimeApi.chainRegistry()
    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    private val rpcCalls = runtimeApi.rpcCalls()

    private val remoteStorage = runtimeApi.remoteStorageSource()

    @Test
    fun testBlockParsing() = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
        val chain = chainRegistry.getChain(chainGenesis)

        val block = rpcCalls.getBlock(chain.id)

        val logTag = this@BlockParsingIntegrationTest.LOG_TAG

        Log.d(logTag, block.block.header.number.toString())

        val events = remoteStorage.query(
            chainId = chain.id,
            keyBuilder = { it.metadata.system().storage("Events").storageKey() },
            binding = { scale, runtime ->
                Log.d(logTag, scale!!)
                bindEventRecords(scale, runtime)
            }
        )

//        val eventsRaw = "0x0800000000000000000000000000000002000000010000000000585f8f0900000000020000"
//        val type = bindEventRecords(eventsRaw, chainRegistry.getRuntime(chain.id))
    }
}
