package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_impl.domain.crosschain.transferConfiguration
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger

class CrossChainTransfersIntegrationTest : BaseIntegrationTest() {

    private val walletApi = FeatureUtils.getFeature<WalletFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        WalletFeatureApi::class.java
    )

    private val chainTransfersRepository = walletApi.crossChainTransfersRepository
    private val crossChainWeigher = walletApi.crossChainWeigher

    @Test
    fun testCrossChainConfig() = runBlocking {
        val moonriverChain = chainRegistry.findChain { it.name == "Moonriver" }!!
        val karInMoonriver = moonriverChain.assetsBySymbol.getValue("xcKAR")

        val karuraChain = chainRegistry.findChain { it.name == "Karura" }!!

        val crossChainConfig = chainTransfersRepository.getConfiguration()

        val crossChainTransfer = crossChainConfig.transferConfiguration(
            originChain = moonriverChain,
            originAsset = karInMoonriver,
            destinationChain = karuraChain,
            destinationParaId = chainTransfersRepository.paraId(karuraChain.id)
        )!!

        val feeResponse = crossChainWeigher.estimateFee(crossChainTransfer)

        error("Destination Fee: ${karInMoonriver.formatAmount(feeResponse.destination!!)}")

        Unit
    }

    private fun Chain.Asset.formatAmount(planks: BigInteger) = "${amountFromPlanks(planks)} $symbol"
}
