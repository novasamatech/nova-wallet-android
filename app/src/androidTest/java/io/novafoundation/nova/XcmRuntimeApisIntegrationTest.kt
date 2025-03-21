package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.toResult
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter.Wild
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.feature_xcm_api.extrinsic.composeXcmExecute
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.BuyExecution
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.DepositAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.DepositReserveAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.WithdrawAsset
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
import io.novafoundation.nova.feature_xcm_api.message.asXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior.*
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.GeneralIndex
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.PalletInstance
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asRelativeLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.getByLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.getInnerSuccessOrThrow
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.getAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class XcmRuntimeApisIntegrationTest : BaseIntegrationTest() {

    private val xcmApi = FeatureUtils.getFeature<XcmFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        XcmFeatureApi::class.java
    )

    private val dryRunApi = xcmApi.dryRunApi

    private val xcmPaymentApi = xcmApi.xcmPaymentApi

    @Test
    fun testDryRunXcmTeleport() = runTest {
        val polkadot = chainRegistry.polkadot()
        val polkadotAssetHub = chainRegistry.polkadotAssetHub()

        val polkadotRuntime = chainRegistry.getRuntime(polkadot.id)

        val polkadotLocation = Here.asLocation()
        val polkadotAssetHubLocation = Junctions(ParachainId(1000)).asLocation()

        val dotLocation = polkadotLocation.toRelative()
        val amount = polkadot.utilityAsset.planksFromAmount(BigDecimal.ONE)
        val assets = MultiAsset.from(dotLocation, amount)

        val origin = "16WWmr2Xqgy5fna35GsNHXMU7vDBM12gzHCFGibQjSmKpAN".toAccountId().intoKey()
        val beneficiary = origin.toMultiLocation()

        val xcmVersion = XcmVersion.V4

        val pahVersionedLocation = polkadotAssetHubLocation.toRelative().versionedXcm(xcmVersion)

        // Compose limited_teleport_assets call to execute on Polkadot
        val call = polkadotRuntime.composeCall(
            moduleName =  polkadotRuntime.metadata.xcmPalletName(),
            callName = "limited_teleport_assets",
            args = mapOf(
                "dest" to pahVersionedLocation.toEncodableInstance(),
                "beneficiary" to beneficiary.versionedXcm(xcmVersion).toEncodableInstance(),
                "assets" to MultiAssets(assets).versionedXcm(xcmVersion).toEncodableInstance(),
                "fee_asset_item" to BigInteger.ZERO,
                "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
            )
        )

        // Dry run call execution
        val dryRunEffects = dryRunApi.dryRunCall(
            originCaller = OriginCaller.System.Signed(origin),
            call = call,
            XcmVersion.V4,
            chainId = polkadot.id
        )
            .getOrThrow()
            .toResult().getOrThrow()

        // Find xcm forwarded to Polkadot Asset Hub
        val forwardedXcm = dryRunEffects.forwardedXcms.getByLocation(pahVersionedLocation).first()
        println(forwardedXcm)

        // Dry run execution of forwarded message on Polkadot Asset Hub
        val xcmDryRunEffects = dryRunApi.dryRunRawXcm(
            xcm = forwardedXcm,
            originLocation = polkadotLocation.fromPointOfViewOf(polkadotAssetHubLocation).versionedXcm(xcmVersion),
            chainId = polkadotAssetHub.id
        )
            .getInnerSuccessOrThrow("XcmRuntimeApisIntegrationTest")

        println(xcmDryRunEffects.emittedEvents)
    }

    @Test
    fun testDryRunUsdtTransfer() = runTest {
        val polkadotAssetHub = chainRegistry.polkadotAssetHub()
        val pahRuntime = chainRegistry.getRuntime(polkadotAssetHub.id)

        val polimec = chainRegistry.getChain(Chain.Geneses.POLIMEC)
        val usdtOnPah = polkadotAssetHub.getAsset(TokenSymbol("USDT"))

        val polkadotAssetHubLocation = Junctions(ParachainId(1000)).asLocation()
        val polimecLocation = Junctions(ParachainId(3344)).asLocation()
        val polimecFromPah = polimecLocation.fromPointOfViewOf(polkadotAssetHubLocation)
        val pahFromPolimec = polkadotAssetHubLocation.fromPointOfViewOf(polimecLocation)

        val usdtLocation = Junctions(ParachainId(1000), PalletInstance(50), GeneralIndex(1984)).asLocation()
        val usdtOnPahLocation = usdtLocation.fromPointOfViewOf(polkadotAssetHubLocation)
        val usdtOnPolimec = usdtLocation.fromPointOfViewOf(polimecLocation)

        val amount = usdtOnPah.planksFromAmount(BigDecimal.ONE)

        val origin = "14QVrx7grkChY7m2FUH2tv647eJbdVV2fJzeA46Z7HYDaJub".toAccountId().intoKey()
        val beneficiary = origin.toMultiLocation()

        val xcmVersion = XcmVersion.V4

        val assets = MultiAsset.from(usdtOnPahLocation, amount)
        val feesOnPah = MultiAsset.from(usdtOnPahLocation, amount / BigInteger.TWO)
        val feesOnPolimec = MultiAsset.from(usdtOnPolimec, amount / BigInteger.TWO)

        val message = XcmMessage(
            WithdrawAsset(assets.intoMultiAssets()),
            BuyExecution(feesOnPah, WeightLimit.Limited(0, 0)),
            DepositReserveAsset(
                assets = Wild.AllCounted(1),
                dest = polimecFromPah,
                xcm = XcmMessage(
                    BuyExecution(feesOnPolimec, WeightLimit.Unlimited),
                    DepositAsset(Wild.AllCounted(1), beneficiary)
                )
            )
        ).versionedXcm(xcmVersion)

        val call = pahRuntime.composeXcmExecute(message, WeightV2.max())

        // Dry run call execution
        val dryRunEffects = dryRunApi.dryRunCall(
            originCaller = OriginCaller.System.Signed(origin),
            call = call,
            chainId = polkadotAssetHub.id,
            resultsXcmVersion = xcmVersion
        )
            .getOrThrow()
            .toResult().getOrThrow()

        println(dryRunEffects.emittedEvents.map { it.label() })
        // Find xcm forwarded to Polkadot Asset Hub
        val forwardedXcm = dryRunEffects.forwardedXcms.getByLocation(polimecFromPah.versionedXcm(xcmVersion)).first()
        println(forwardedXcm)

        // Dry run execution of forwarded message on Polimec
        val xcmDryRunEffects = dryRunApi.dryRunRawXcm(
            xcm = forwardedXcm,
            originLocation = pahFromPolimec.versionedXcm(xcmVersion),
            chainId = polimec.id
        )
            .getInnerSuccessOrThrow("XcmRuntimeApisIntegrationTest")

        println(xcmDryRunEffects.emittedEvents.map { it.label() })
    }

    private fun GenericEvent.Instance.label(): String {
        return "${module.name}.${event.name}"
    }

    @Test
    fun testQueryXcmWeight() = runTest {
        val polkadot = chainRegistry.polkadot()
        val multiAsset = MultiAsset.from(Here.asRelativeLocation(), amount = BigInteger.ONE)

        val beneficiary = "16WWmr2Xqgy5fna35GsNHXMU7vDBM12gzHCFGibQjSmKpAN".toAccountId().intoKey().toMultiLocation()

        val xcmMessage = listOf(
            WithdrawAsset(multiAsset.intoMultiAssets()),
            DepositAsset(Wild.AllCounted(1), beneficiary)
        ).asXcmMessage().versionedXcm(XcmVersion.V4)

        val queryWeightResult = xcmPaymentApi.queryXcmWeight(polkadot.id, xcmMessage)
            .getInnerSuccessOrThrow("XcmRuntimeApisIntegrationTest")
        println(queryWeightResult)
    }
}
