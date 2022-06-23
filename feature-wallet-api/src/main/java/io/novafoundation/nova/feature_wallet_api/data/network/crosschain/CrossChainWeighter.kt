package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import java.math.BigInteger

data class CrossChainFee(
    val reserve: BigInteger?,
    val destination: BigInteger?
)

interface CrossChainWeigher {

    suspend fun estimateRequiredDestWeight(transferConfiguration: CrossChainTransferConfiguration): Weight

    suspend fun estimateFee(transferConfiguration: CrossChainTransferConfiguration): CrossChainFee
}
