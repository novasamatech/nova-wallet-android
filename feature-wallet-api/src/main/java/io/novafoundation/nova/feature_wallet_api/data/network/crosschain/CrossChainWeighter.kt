package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration

interface CrossChainWeigher {

    suspend fun estimateFee(transfer: AssetTransferBase, config: CrossChainTransferConfiguration): CrossChainFeeModel
}
