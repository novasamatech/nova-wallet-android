package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

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
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration
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
