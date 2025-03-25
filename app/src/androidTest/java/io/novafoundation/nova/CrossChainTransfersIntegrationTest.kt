package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.emptySubstrateAccountId
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.model.toDefaultSubstrateAddress
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.repository.getXcmChain
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.transferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.transferConfiguration
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.normalizeTokenSymbol
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
            val asssetInOrigin = originChain.assets.first { it.symbol.value == what }

            val destinationChain = chainRegistry.findChain { it.name == to }!!
            val asssetInDestination = destinationChain.assets.first { normalizeTokenSymbol(it.symbol.value) == normalizeTokenSymbol(what) }

            val crossChainConfig = chainTransfersRepository.getConfiguration()

            val crossChainTransfer = crossChainConfig.transferConfiguration(
                originChain = parachainInfoRepository.getXcmChain(originChain),
                originAsset = asssetInOrigin,
                destinationChain = parachainInfoRepository.getXcmChain(destinationChain),
            )!!

            val transfer = AssetTransferBase(
                recipient = originChain.addressOf(originChain.emptyAccountId()),
                originChain = originChain,
                originChainAsset = asssetInOrigin,
                destinationChain = destinationChain,
                destinationChainAsset = asssetInDestination,
                feePaymentCurrency = FeePaymentCurrency.Native,
                amountPlanks = BigInteger.ZERO
            )

            val crossChainFeeResult = runCatching { crossChainWeigher.estimateFee(transfer, crossChainTransfer) }

            check(crossChainFeeResult.isSuccess)
        }
    }
}
