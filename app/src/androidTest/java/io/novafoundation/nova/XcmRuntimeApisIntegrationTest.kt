package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.toResult
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.asXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior.*
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
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
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
        val xcmDryRunEffects = dryRunApi.dryRunXcm(
            xcm = forwardedXcm,
            originLocation = polkadotLocation.fromPointOfViewOf(polkadotAssetHubLocation).versionedXcm(xcmVersion),
            chainId = polkadotAssetHub.id
        )
            .getInnerSuccessOrThrow("XcmRuntimeApisIntegrationTest")

        println(xcmDryRunEffects.emittedEvents)
    }

    @Test
    fun testQueryXcmWeight() = runTest {
        val polkadot = chainRegistry.polkadot()
        val multiAsset = MultiAsset.from(Here.asRelativeLocation(), amount = BigInteger.ONE)

        val beneficiary = "16WWmr2Xqgy5fna35GsNHXMU7vDBM12gzHCFGibQjSmKpAN".toAccountId().intoKey().toMultiLocation()

        val xcmMessage = listOf(
            XcmInstruction.WithdrawAsset(multiAsset.intoMultiAssets()),
            XcmInstruction.DepositAsset(MultiAssetFilter.Wild.AllCounted(1), beneficiary)
        ).asXcmMessage().versionedXcm(XcmVersion.V4)

        val queryWeightResult = xcmPaymentApi.queryXcmWeight(polkadot.id, xcmMessage)
            .getInnerSuccessOrThrow("XcmRuntimeApisIntegrationTest")
        println(queryWeightResult)
    }
}
