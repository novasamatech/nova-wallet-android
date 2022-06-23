package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.implementations.accountIdToMultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.implementations.plus
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class RealCrossChainTransactor(
    private val weigher: CrossChainWeigher,
    private val extrinsicService: ExtrinsicService,
) : CrossChainTransactor {

    override suspend fun estimateOriginFee(configuration: CrossChainTransferConfiguration, transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.originChain) {
            crossChainTransfer(configuration, transfer, crossChainFee = Balance.ZERO)
        }
    }

    override suspend fun performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer,
        crossChainFee: BigInteger
    ): Result<*> {
        return extrinsicService.submitExtrinsic(transfer.originChain) {
            crossChainTransfer(configuration, transfer, crossChainFee)
        }
    }

    private suspend fun ExtrinsicBuilder.crossChainTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer,
        crossChainFee: BigInteger
    ) {
        when (configuration.transferType) {
            XcmTransferType.X_TOKENS -> xTokensTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET -> xcmPalletTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.UNKNOWN -> throw IllegalArgumentException("Unknown transfer type")
        }
    }

    private suspend fun ExtrinsicBuilder.xTokensTransfer(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransfer,
        crossChainFee: BigInteger
    ) {
        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)
        val fullDestinationLocation = configuration.destinationChainLocation + assetTransfer.beneficiaryLocation()

        call(
            moduleName = Modules.X_TOKENS,
            callName = "transfer_multiasset",
            arguments = mapOf(
                "asset" to VersionedMultiAsset.V1(multiAsset).toEncodableInstance(),
                "dest" to fullDestinationLocation.versioned().toEncodableInstance(),
                "dest_weight" to weigher.estimateRequiredDestWeight(configuration)
            )
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTransfer(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransfer,
        crossChainFee: BigInteger
    ) {
        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)

        call(
            moduleName = runtime.metadata.xcmPalletName(),
            callName = "limited_reserve_transfer_assets",
            arguments = mapOf(
                "dest" to configuration.destinationChainLocation.versioned().toEncodableInstance(),
                "beneficiary" to assetTransfer.beneficiaryLocation().versioned().toEncodableInstance(),
                "assets" to VersionedMultiAssets.V1(listOf(multiAsset)).toEncodableInstance(),
                "fee_asset_item" to BigInteger.ZERO,
                "weight_limit" to WeightLimit.Limited(weigher.estimateRequiredDestWeight(configuration)).toEncodableInstance()
            )
        )
    }

    private fun CrossChainTransferConfiguration.multiAssetFor(
        transfer: AssetTransfer,
        crossChainFee: BigInteger
    ): XcmMultiAsset {
        // we add cross chain fee top of entered amount so received amount will be no less than entered one
        val planks = transfer.originChainAsset.planksFromAmount(transfer.amount) + crossChainFee

        return XcmMultiAsset.from(assetLocation, planks)
    }

    private fun AssetTransfer.beneficiaryLocation(): MultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient)

        return accountId.accountIdToMultiLocation()
    }
}
