package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

open class UnsupportedAssetTransfers : AssetTransfers {

    override fun getValidationSystem(coroutineScope: CoroutineScope): AssetTransfersValidationSystem {
        throw UnsupportedOperationException("Unsupported")
    }

    override suspend fun calculateFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): Fee {
        throw UnsupportedOperationException("Unsupported")
    }

    override suspend fun performTransfer(transfer: WeightedAssetTransfer, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission> {
        return Result.failure(UnsupportedOperationException("Unsupported"))
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return false
    }
}
