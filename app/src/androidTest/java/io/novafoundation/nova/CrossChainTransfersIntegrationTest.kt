package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.implementations.transferConfiguration
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
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

    private val parachainInfoRepository = runtimeApi.parachainInfoRepository

    @Test
    fun testParachainToParachain() = performFeeTest(
        from = "Moonriver",
        what = "xcKAR",
        to = "Karura"
    )

    @Test
    fun testRelaychainToParachain() = performFeeTest(
        from = "Kusama",
        what = "KSM",
        to = "Moonriver"
    )

    @Test
    fun testParachainToRelaychain() = performFeeTest(
        from = "Moonriver",
        what = "xcKSM",
        to = "Kusama"
    )

    @Test
    fun testParachainToParachainNonReserve() = performFeeTest(
        from = "Karura",
        what = "BNC",
        to = "Moonriver"
    )

    private fun performFeeTest(
        from: String,
        to: String,
        what: String
    ) {
        runBlocking {
            val originChain = chainRegistry.findChain { it.name == from }!!
            val asssetInOrigin = originChain.assets.find { it.symbol == what }!!

            val destinationChain = chainRegistry.findChain { it.name == to }!!

            val crossChainConfig = chainTransfersRepository.getConfiguration()

            val crossChainTransfer = crossChainConfig.transferConfiguration(
                originChain = originChain,
                originAsset = asssetInOrigin,
                destinationChain = destinationChain,
                destinationParaId = parachainInfoRepository.paraId(destinationChain.id)
            )!!

            val crossChainFee = crossChainWeigher.estimateFee(crossChainTransfer)

            error(crossChainFee.formatWith(asssetInOrigin))
        }
    }

    private fun CrossChainFeeModel.formatWith(
        transferringAsset: Chain.Asset
    ): String {
        fun BigInteger?.formatAmount() = this?.let { it.formatPlanks(transferringAsset) }

        return """
            
            Destination Fee: ${destination?.formatAmount()}
            Reserve Fee: ${reserve?.formatAmount()}
            Total XCM Fee: ${(reserve.orZero() + destination.orZero()).formatAmount()}
        """.trimIndent()
    }
}
