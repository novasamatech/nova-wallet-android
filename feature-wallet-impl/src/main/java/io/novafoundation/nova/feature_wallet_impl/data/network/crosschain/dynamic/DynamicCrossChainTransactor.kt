package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.from
import io.novafoundation.nova.feature_xcm_api.multiLocation.plus
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.ZERO

@FeatureScope
class DynamicCrossChainTransactor @Inject constructor(
    private val xcmVersionDetector: XcmVersionDetector,
) {

    context(ExtrinsicBuilder)
    suspend fun crossChainTransfer(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransferAssets(configuration, transfer, crossChainFee)
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTransferAssets(
        configuration: DynamicCrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransfer(
            configuration = configuration,
            assetTransfer = assetTransfer,
            crossChainFee = crossChainFee,
            callName = "transfer_assets"
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTransfer(
        configuration: DynamicCrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance,
        callName: String
    ) {
        val lowestMultiLocationVersion = xcmVersionDetector.lowestPresentMultiLocationVersion(assetTransfer.originChain.id).orDefault()
        val lowestMultiAssetsVersion = xcmVersionDetector.lowestPresentMultiAssetsVersion(assetTransfer.originChain.id).orDefault()

        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)

        call(
            moduleName = runtime.metadata.xcmPalletName(),
            callName = callName,
            arguments = mapOf(
                "dest" to configuration.destinationChainLocation.versionedXcm(lowestMultiLocationVersion).toEncodableInstance(),
                "beneficiary" to assetTransfer.beneficiaryLocation().versionedXcm(lowestMultiLocationVersion).toEncodableInstance(),
                "assets" to MultiAssets(multiAsset).versionedXcm(lowestMultiAssetsVersion).toEncodableInstance(),
                "fee_asset_item" to BigInteger.ZERO,
                "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
            )
        )
    }

    private fun DynamicCrossChainTransferConfiguration.multiAssetFor(
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): MultiAsset {
        // we add cross chain fee top of entered amount so received amount will be no less than entered one
        val planks = transfer.amountPlanks + crossChainFee

        return MultiAsset.from(assetLocation, planks)
    }

    private fun AssetTransferBase.beneficiaryLocation(): RelativeMultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient).intoKey()
        return accountId.toMultiLocation()
    }
}
