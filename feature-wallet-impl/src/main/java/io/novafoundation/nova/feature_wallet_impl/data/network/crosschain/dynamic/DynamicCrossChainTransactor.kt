package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.destinationChainLocationOnOrigin
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger
import javax.inject.Inject

private val USED_XCM_VERSION = XcmVersion.V4

@FeatureScope
class DynamicCrossChainTransactor @Inject constructor(
    private val chainRegistry: ChainRegistry
) {

    context(ExtrinsicBuilder)
    suspend fun crossChainTransfer(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        val call = composeCrossChainTransferCall(configuration, transfer, crossChainFee)
        call(call)
    }

    suspend fun composeCrossChainTransferCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): GenericCall.Instance {
        val totalTransferAmount = transfer.amountPlanks + crossChainFee
        val multiAsset =  MultiAsset.from(configuration.assetLocationOnOrigin, totalTransferAmount)

        return chainRegistry.withRuntime(configuration.originChainId) {
            composeCall(
                moduleName = metadata.xcmPalletName(),
                callName = "transfer_assets",
                args = mapOf(
                    "dest" to configuration.destinationChainLocationOnOrigin().versionedXcm().toEncodableInstance(),
                    "beneficiary" to transfer.beneficiaryLocation().versionedXcm().toEncodableInstance(),
                    "assets" to MultiAssets(multiAsset).versionedXcm().toEncodableInstance(),
                    "fee_asset_item" to BigInteger.ZERO,
                    "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
                )
            )
        }
    }

    private fun <T> T.versionedXcm() = versionedXcm(USED_XCM_VERSION)

    private fun AssetTransferBase.beneficiaryLocation(): RelativeMultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient).intoKey()
        return accountId.toMultiLocation()
    }
}
