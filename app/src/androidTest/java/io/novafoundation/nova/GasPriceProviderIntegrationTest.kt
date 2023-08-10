package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.runtime.ethereum.gas.LegacyGasPriceProvider
import io.novafoundation.nova.runtime.ethereum.gas.MaxPriorityFeeGasProvider
import io.novafoundation.nova.runtime.ext.Ids
import io.novafoundation.nova.runtime.multiNetwork.awaitCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Test

class GasPriceProviderIntegrationTest : BaseIntegrationTest() {

    @Test
    fun compareLegacyAndImprovedGasPriceEstimations() = runTest {
        val api = chainRegistry.awaitCallEthereumApiOrThrow(Chain.Ids.MOONBEAM)

        val legacy = LegacyGasPriceProvider(api)
        val improved = MaxPriorityFeeGasProvider(api)

        val legacyResult = legacy.getGasPrice()
        val improvedResult = improved.getGasPrice()

        Log.d("GasPriceProviderIntegrationTest", """
            Legacy:   $legacyResult
            Improved: $improvedResult
        """.trimIndent() )
    }
}
