package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration

interface CrossChainWeigher {

    suspend fun estimateRequiredDestWeight(transferConfiguration: CrossChainTransferConfiguration): Weight

    suspend fun estimateFee(amount: Balance, config: CrossChainTransferConfiguration): CrossChainFeeModel
}
