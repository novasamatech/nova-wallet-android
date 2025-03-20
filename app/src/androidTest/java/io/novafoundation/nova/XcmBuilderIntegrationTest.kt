package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.builder.depositAllAssetsTo
import io.novafoundation.nova.feature_xcm_api.builder.payFeesIn
import io.novafoundation.nova.feature_xcm_api.builder.withdrawAsset
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior.*
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import org.junit.Test

class XcmBuilderIntegrationTest : BaseIntegrationTest() {

    private val xcmApi = FeatureUtils.getFeature<XcmFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        XcmFeatureApi::class.java
    )

    private val walletApi = FeatureUtils.getFeature<WalletFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        WalletFeatureApi::class.java
    )

    val assetIssuerRegistry = walletApi.assetIssuerRegistry
    val xcmBuilderFactory = xcmApi.xcmBuilderFactory

    @Test
    fun testXcmBuilder() = runTest {
        val westend = chainRegistry.getChain(Chain.Geneses.WESTEND)
        val wah = chainRegistry.getChain(Chain.Geneses.WESTEND_ASSET_HUB)

        val wndOnWestend = westend.utilityAsset
        val wndOnWah = wah.utilityAsset

        val wndLocation = Here.asLocation()

        val westendLocation = ChainLocation(westend.id, Here.asLocation())
        val wahLocation = ChainLocation(wah.id, Junctions(ParachainId(1000)).asLocation())

        val xcmBuilder = xcmBuilderFactory.create(
            initial = westendLocation,
            xcmVersion = XcmVersion.V5,
            measureXcmFees = xcmBuilderFactory.dryRunMeasureFees(assetIssuerRegistry)
        )

        val amount = wndOnWestend.planksFromAmount(10.toBigDecimal())
        val origin = "16WWmr2Xqgy5fna35GsNHXMU7vDBM12gzHCFGibQjSmKpAN".toAccountId().intoKey()

        val xcmMessage = xcmBuilder.apply {
            // Polkadot
            withdrawAsset(wndLocation, amount)
            payFeesIn(AssetLocation(wndOnWestend.fullId, wndLocation))
            initiateTeleport(MultiAssetFilter.Wild.AllCounted(1), wahLocation)

            // Wah
            payFeesIn(AssetLocation(wndOnWah.fullId, wndLocation))
            depositAsset(MultiAssetFilter.Wild.AllCounted(1), origin)
        }.build()

        println(xcmMessage)
    }
}
