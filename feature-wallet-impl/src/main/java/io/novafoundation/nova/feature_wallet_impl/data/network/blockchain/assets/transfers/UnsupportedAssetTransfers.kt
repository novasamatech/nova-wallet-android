package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

open class UnsupportedAssetTransfers : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem
        get() = throw UnsupportedOperationException("Unsupported")

    override suspend fun totalCanDropBelowMinimumBalance(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override fun totalCanDropBelowMinimumBalanceFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf(true)
    }

    override suspend fun calculateFee(transfer: AssetTransfer): Fee {
        throw UnsupportedOperationException("Unsupported")
    }

    override suspend fun performTransfer(transfer: WeightedAssetTransfer): Result<String> {
        return Result.failure(UnsupportedOperationException("Unsupported"))
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return false
    }
}
