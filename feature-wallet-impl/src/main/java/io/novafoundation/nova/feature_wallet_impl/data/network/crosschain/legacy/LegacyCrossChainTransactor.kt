package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import android.util.Log
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.transformResult
import io.novafoundation.nova.common.utils.wrapInResult
import io.novafoundation.nova.common.utils.xTokensName
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.tryDetectDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.replaceAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.XcmTransferType
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
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findRelayChainOrThrow
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.BlockEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.hasEvent
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeout
import java.math.BigInteger
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

@FeatureScope
class LegacyCrossChainTransactor @Inject constructor(
    private val weigher: LegacyCrossChainWeigher,
    private val xcmVersionDetector: XcmVersionDetector,
) {

    context(ExtrinsicBuilder)
    suspend fun crossChainTransfer(
        configuration: LegacyCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        when (configuration.transferType) {
            XcmTransferType.X_TOKENS -> xTokensTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET_RESERVE -> xcmPalletReserveTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET_TELEPORT -> xcmPalletTeleport(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET_TRANSFER_ASSETS -> xcmPalletTransferAssets(configuration, transfer, crossChainFee)
            XcmTransferType.UNKNOWN -> throw IllegalArgumentException("Unknown transfer type")
        }
    }


    private suspend fun ExtrinsicBuilder.xTokensTransfer(
        configuration: LegacyCrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)
        val fullDestinationLocation = configuration.destinationChainLocation + assetTransfer.beneficiaryLocation()
        val requiredDestWeight = weigher.estimateRequiredDestWeight(configuration)

        val lowestMultiLocationVersion = xcmVersionDetector.lowestPresentMultiLocationVersion(assetTransfer.originChain.id).orDefault()
        val lowestMultiAssetVersion = xcmVersionDetector.lowestPresentMultiAssetVersion(assetTransfer.originChain.id).orDefault()

        call(
            moduleName = runtime.metadata.xTokensName(),
            callName = "transfer_multiasset",
            arguments = mapOf(
                "asset" to multiAsset.versionedXcm(lowestMultiAssetVersion).toEncodableInstance(),
                "dest" to fullDestinationLocation.versionedXcm(lowestMultiLocationVersion).toEncodableInstance(),

                // depending on the version of the pallet, only one of weights arguments going to be encoded
                "dest_weight" to destWeightEncodable(requiredDestWeight),
                "dest_weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
            )
        )
    }

    private fun destWeightEncodable(weight: Weight): Any = weight

    private suspend fun ExtrinsicBuilder.xcmPalletTransferAssets(
        configuration: LegacyCrossChainTransferConfiguration,
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

    private suspend fun ExtrinsicBuilder.xcmPalletReserveTransfer(
        configuration: LegacyCrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransfer(
            configuration = configuration,
            assetTransfer = assetTransfer,
            crossChainFee = crossChainFee,
            callName = "limited_reserve_transfer_assets"
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTeleport(
        configuration: LegacyCrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransfer(
            configuration = configuration,
            assetTransfer = assetTransfer,
            crossChainFee = crossChainFee,
            callName = "limited_teleport_assets"
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTransfer(
        configuration: LegacyCrossChainTransferConfiguration,
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

    private fun LegacyCrossChainTransferConfiguration.multiAssetFor(
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
