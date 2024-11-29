package io.novafoundation.nova

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.data.network.runtime.binding.bindEventRecords
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockParsingIntegrationTest {

    private val chainGenesis = "f1cf9022c7ebb34b162d5b5e34e705a5a740b2d0ecc1009fb89023e62a488108" // Shiden

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
                bindEventRecords(scale)
            }
        )

//        val eventsRaw = "0x0800000000000000000000000000000002000000010000000000585f8f0900000000020000"
//        val type = bindEventRecords(eventsRaw, chainRegistry.getRuntime(chain.id))
    }
}
