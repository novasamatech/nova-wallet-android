package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import java.math.BigInteger

interface CrossChainTransactor {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun estimateOriginFee(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer
    ): BigInteger

    suspend fun performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer,
        crossChainFee: BigInteger,
    ): Result<*>
}
