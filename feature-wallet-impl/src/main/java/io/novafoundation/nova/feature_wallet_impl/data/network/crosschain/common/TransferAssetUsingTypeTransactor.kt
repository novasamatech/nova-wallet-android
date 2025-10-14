package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.common

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.assetLocationOnOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.destinationChainLocationOnOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.originChainId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.buildXcmWithoutFeesMeasurement
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

@FeatureScope
class TransferAssetUsingTypeTransactor @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val xcmBuilderFactory: XcmBuilder.Factory,
    private val xcmVersionDetector: XcmVersionDetector,
) {

    suspend fun composeCall(
        configuration: CrossChainTransferConfigurationBase,
        transfer: AssetTransferBase,
        crossChainFee: Balance,
        forceXcmVersion: XcmVersion? = null
    ): GenericCall.Instance {
        val totalTransferAmount = transfer.amountPlanks + crossChainFee
        val multiAsset = MultiAsset.from(configuration.assetLocationOnOrigin(), totalTransferAmount)
        val multiAssetId = MultiAssetId(configuration.assetLocationOnOrigin())

        val multiLocationVersion = forceXcmVersion ?: xcmVersionDetector.lowestPresentMultiLocationVersion(transfer.originChain.id).orDefault()
        val multiAssetsVersion = forceXcmVersion ?: xcmVersionDetector.lowestPresentMultiAssetsVersion(transfer.originChain.id).orDefault()
        val multiAssetIdVersion = forceXcmVersion ?: xcmVersionDetector.lowestPresentMultiAssetIdVersion(transfer.originChain.id).orDefault()

        val transferTypeParam = configuration.transferTypeParam(multiAssetsVersion)

        return chainRegistry.withRuntime(configuration.originChainId) {
            composeCall(
                moduleName = metadata.xcmPalletName(),
                callName = "transfer_assets_using_type_and_then",
                arguments = mapOf(
                    "dest" to configuration.destinationChainLocationOnOrigin().versionedXcm(multiLocationVersion).toEncodableInstance(),
                    "assets" to MultiAssets(multiAsset).versionedXcm(multiAssetsVersion).toEncodableInstance(),
                    "assets_transfer_type" to transferTypeParam,
                    "remote_fees_id" to multiAssetId.versionedXcm(multiAssetIdVersion).toEncodableInstance(),
                    "fees_transfer_type" to transferTypeParam,
                    "custom_xcm_on_dest" to constructCustomXcmOnDest(configuration, transfer, multiLocationVersion).toEncodableInstance(),
                    "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
                )
            )
        }
    }

    private fun CrossChainTransferConfigurationBase.transferTypeParam(locationXcmVersion: XcmVersion): Any {
        return when (val type = transferType) {
            is XcmTransferType.Teleport -> DictEnum.Entry("Teleport", null)

            is XcmTransferType.Reserve.Destination -> DictEnum.Entry("DestinationReserve", null)

            is XcmTransferType.Reserve.Origin -> DictEnum.Entry("LocalReserve", null)

            is XcmTransferType.Reserve.Remote -> {
                val reserveChainRelative = type.remoteReserveLocation.location.fromPointOfViewOf(originChainLocation.location)
                val remoteReserveEncodable = reserveChainRelative.versionedXcm(locationXcmVersion).toEncodableInstance()

                DictEnum.Entry("RemoteReserve", remoteReserveEncodable)
            }
        }
    }

    private suspend fun constructCustomXcmOnDest(
        configuration: CrossChainTransferConfigurationBase,
        transfer: AssetTransferBase,
        minDetectedXcmVersion: XcmVersion
    ): VersionedXcmMessage {
        return xcmBuilderFactory.buildXcmWithoutFeesMeasurement(
            initial = configuration.originChainLocation,
            // singleCounted is only available from V3
            xcmVersion = minDetectedXcmVersion.coerceAtLeast(XcmVersion.V3)
        ) {
            depositAsset(MultiAssetFilter.singleCounted(), transfer.recipientAccountId)
        }
    }
}
