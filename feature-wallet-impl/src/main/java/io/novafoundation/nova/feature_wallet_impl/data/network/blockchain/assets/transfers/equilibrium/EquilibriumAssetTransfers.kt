package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.equilibrium

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class EquilibriumAssetTransfers : AssetTransfers {
    override val validationSystem: AssetTransfersValidationSystem
        get() = ValidationSystem { }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return BigInteger.ZERO
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        return Result.failure(Exception())
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return false
    }
}
