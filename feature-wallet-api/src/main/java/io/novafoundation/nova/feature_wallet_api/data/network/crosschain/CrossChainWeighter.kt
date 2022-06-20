package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import java.math.BigInteger

data class CrossChainFee(
    val origin: BigInteger,
    val reserve: BigInteger?,
    val destination: BigInteger?
)

interface CrossChainWeigher {

    suspend fun estimateFee(transferConfiguration: CrossChainTransferConfiguration): CrossChainFee
}
