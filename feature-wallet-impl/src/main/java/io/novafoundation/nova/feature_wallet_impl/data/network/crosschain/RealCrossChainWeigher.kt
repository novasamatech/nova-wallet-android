package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainWeigher
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainWeigher

class RealCrossChainWeigher(
    private val dynamic: DynamicCrossChainWeigher,
    private val legacy: LegacyCrossChainWeigher
) : CrossChainWeigher {

    override suspend fun estimateFee(transfer: AssetTransferBase, config: CrossChainTransferConfiguration): CrossChainFeeModel {
        return when (config) {
            is CrossChainTransferConfiguration.Dynamic -> dynamic.estimateFee(config.config, transfer)
            is CrossChainTransferConfiguration.Legacy -> legacy.estimateFee(transfer.amountPlanks, config.config)
        }
    }
}
