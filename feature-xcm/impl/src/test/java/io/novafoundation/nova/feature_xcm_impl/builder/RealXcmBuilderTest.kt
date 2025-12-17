package io.novafoundation.nova.feature_xcm_impl.builder

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter.Wild.All
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.withAmount
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.buyExecution
import io.novafoundation.nova.feature_xcm_api.builder.depositAllAssetsTo
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.builder.payFees
import io.novafoundation.nova.feature_xcm_api.builder.payFeesIn
import io.novafoundation.nova.feature_xcm_api.builder.transferReserveAsset
import io.novafoundation.nova.feature_xcm_api.builder.withdrawAsset
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.BuyExecution
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.DepositAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.DepositReserveAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.InitiateReserveWithdraw
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.PayFees
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.TransferReserveAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.WithdrawAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior.Here
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger


class RealXcmBuilderTest {

    val amount = 100000000000000.toBigInteger()
    val buyExecutionFees = amount / 2.toBigInteger()

    val testMeasuredFees = 10.toBigInteger()
    val testExactFees = 11.toBigInteger()

    val recipient = ByteArray(32) { 1 }.intoKey()

    val polkadot = ChainLocation(Chain.Geneses.POLKADOT, Here.asLocation())
    val pah = ChainLocation(Chain.Geneses.POLKADOT_ASSET_HUB, ParachainId(1000).asLocation())
    val hydration = ChainLocation(Chain.Geneses.HYDRA_DX, ParachainId(2034).asLocation())

    val dot =  Here.asLocation()
    val dotLocation = AssetLocation(FullChainAssetId(Chain.Geneses.POLKADOT, 0), dot)
    val dotOnPolkadot = MultiAssetId(dot.fromPointOfViewOf(polkadot.location))
    val dotOnPah = MultiAssetId(dot.fromPointOfViewOf(pah.location))
    val dotOnHydration = MultiAssetId(dot.fromPointOfViewOf(hydration.location))


    val xcmVersion = XcmVersion.V4

    @Test
    fun `should build empty message`() = runBlocking {
        val expected = XcmMessage(emptyList())

        val result = createBuilder(polkadot).build()

        assertXcmMessageEquals(expected, result)
    }

    @Test
    fun `should build single chain message`() = runBlocking {
        val expected = XcmMessage(
            BuyExecution(dotOnPolkadot.withAmount(amount), WeightLimit.Unlimited),
            DepositAsset(All, recipient.toMultiLocation())
        )

        val result = createBuilder(polkadot).apply {
            buyExecution(dot, amount, WeightLimit.Unlimited)
            depositAllAssetsTo(recipient)
        }.build()

        assertXcmMessageEquals(expected, result)
    }

    @Test
    fun `should perform single context switch`() = runBlocking {
        val forwardedToHydration = XcmMessage(
            BuyExecution(dotOnHydration.withAmount(buyExecutionFees), WeightLimit.Unlimited),
            DepositAsset(All, recipient.toMultiLocation())
        )
        val expectedOnPolkadot = XcmMessage(
            TransferReserveAsset(
                assets = dotOnPolkadot.withAmount(amount).intoMultiAssets(),
                dest = hydration.location.fromPointOfViewOf(polkadot.location),
                xcm = forwardedToHydration
            )
        )

        val result = createBuilder(polkadot).apply {
            // polkadot
            transferReserveAsset(dot, amount, hydration)

            // hydration
            buyExecution(dot, buyExecutionFees, WeightLimit.Unlimited)
            depositAllAssetsTo(recipient)
        }.build()

        assertXcmMessageEquals(expectedOnPolkadot, result)
    }

    @Test
    fun `should perform multiple context switches`() = runBlocking {
        val forwardedToHydration = XcmMessage(
            BuyExecution(dotOnHydration.withAmount(buyExecutionFees), WeightLimit.Unlimited),
            DepositAsset(All, recipient.toMultiLocation())
        )
        val forwardedToPolkadot = XcmMessage(
            BuyExecution(dotOnPolkadot.withAmount(buyExecutionFees), WeightLimit.Unlimited),
            DepositReserveAsset(
                assets = All,
                dest = hydration.location.fromPointOfViewOf(polkadot.location),
                xcm = forwardedToHydration
            )
        )
        val expectedOnPah = XcmMessage(
            WithdrawAsset(dotOnPah.withAmount(amount).intoMultiAssets()),
            InitiateReserveWithdraw(
                assets = All,
                reserve = polkadot.location.fromPointOfViewOf(pah.location),
                xcm = forwardedToPolkadot
            )
        )

        val result = createBuilder(pah).apply {
            // on Pah
            withdrawAsset(dot, amount)
            initiateReserveWithdraw(All, reserve = polkadot)

            // on Polkadot
            buyExecution(dot, buyExecutionFees, WeightLimit.Unlimited)
            depositReserveAsset(All, dest = hydration)

            // on Hydration
            buyExecution(dot, buyExecutionFees, WeightLimit.Unlimited)
            depositAsset(All, recipient)
        }.build()

        assertXcmMessageEquals(expectedOnPah, result)
    }

    @Test
    fun `should set PayFees in exact mode`() = runBlocking {
        val expected = XcmMessage(
            PayFees(dotOnPolkadot.withAmount(testExactFees)),
            DepositAsset(All, recipient.toMultiLocation())
        )

        val result = createBuilder(polkadot).apply {
            payFees(dotOnPolkadot, testExactFees)
            depositAllAssetsTo(recipient)
        }.build()

        assertXcmMessageEquals(expected, result)
    }

    @Test
    fun `should set PayFees in measured mode`() = runBlocking {
        val expected = XcmMessage(
            PayFees(dotOnPolkadot.withAmount(testMeasuredFees)),
            DepositAsset(All, recipient.toMultiLocation())
        )
        val expectedForMeasure = XcmMessage(
            PayFees(dotOnPolkadot.withAmount(BigInteger.ONE)),
            DepositAsset(All, recipient.toMultiLocation())
        )

        val result = createBuilder(polkadot, validateMeasuringMessage = expectedForMeasure).apply {
            payFeesIn(dotLocation)
            depositAllAssetsTo(recipient)
        }.build()

        assertXcmMessageEquals(expected, result)
    }

    private fun assertXcmMessageEquals(expected: XcmMessage, actual: VersionedXcmMessage) {
        assertEquals(expected.versionedXcm(xcmVersion), actual)
    }

    private fun createBuilder(
        origin: ChainLocation,
        validateMeasuringMessage: XcmMessage? = null
    ): XcmBuilder {
        return RealXcmBuilder(origin, XcmVersion.V4, TestMeasureFees(validateMeasuringMessage))
    }

    private inner class TestMeasureFees(
        private val validateMeasuringMessage: XcmMessage?
    ) : MeasureXcmFees {

        override suspend fun measureFees(
            message: VersionedXcmMessage,
            feeAsset: AssetLocation,
            chainLocation: ChainLocation
        ): BalanceOf {
            validateMeasuringMessage?.let { assertXcmMessageEquals(validateMeasuringMessage, message) }
            return testMeasuredFees
        }
    }
}
