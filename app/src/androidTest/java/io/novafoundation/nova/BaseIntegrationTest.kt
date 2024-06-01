package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.withChildScope
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class BaseIntegrationTest(private val allowChainRegistryConnections: Boolean = true) {

    protected val context: Context = ApplicationProvider.getApplicationContext()

    protected val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(context, RuntimeApi::class.java)

    protected val commonApi = FeatureUtils.getCommonApi(context)

    val chainRegistry = runtimeApi.chainRegistry()

    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    @Before
    fun setup() = runBlocking {
        if (allowChainRegistryConnections) {
            externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
        } else {
            externalRequirementFlow.emit(ChainConnection.ExternalRequirement.STOPPED)
        }
    }

    protected fun runTest(action: suspend CoroutineScope.() -> Unit) {
        runBlocking {
            withChildScope {
                action()
            }
        }
    }

    protected suspend fun ChainRegistry.polkadot(): Chain {
        return getChain(Chain.Geneses.POLKADOT)
    }
}
