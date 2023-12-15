package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.utils.average
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.runtime.ethereum.gas.LegacyGasPriceProvider
import io.novafoundation.nova.runtime.ethereum.gas.MaxPriorityFeeGasProvider
import io.novafoundation.nova.runtime.ext.Ids
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.junit.Test
import java.math.BigInteger

class GasPriceProviderIntegrationTest : BaseIntegrationTest() {

    @Test
    fun compareLegacyAndImprovedGasPriceEstimations() = runTest {
        val api = chainRegistry.getCallEthereumApiOrThrow(Chain.Ids.MOONBEAM)

        val legacy = LegacyGasPriceProvider(api)
        val improved = MaxPriorityFeeGasProvider(api)

        val legacyStats = mutableSetOf<BigInteger>()
        val improvedStats = mutableSetOf<BigInteger>()

        api.newHeadsFlow().map {
            legacyStats.add(legacy.getGasPrice())
            improvedStats.add(improved.getGasPrice())
        }
            .take(1000)
            .collect()

        legacyStats.printStats("Legacy")
        improvedStats.printStats("Improved")
    }

    private fun Set<BigInteger>.printStats(name: String) {
        val min = min()
        val max = max()

        Log.d("GasPriceProviderIntegrationTest", """
            Stats for $name source
                Min:  $min
                Max:  $max
                Avg:  ${average()}
                Max/Min ratio: ${max.divideToDecimal(min)}
            """)
    }
}
