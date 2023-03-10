package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmNative

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class EvmNativeAssetTransfers : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem = ValidationSystem {
        validAddress()

        positiveAmount()

        sufficientTransferableBalanceToPayOriginFee()
        sufficientBalanceInUsedAsset()
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        TODO("Evm native asset transfers")
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        TODO("Evm native asset transfers")
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return true
    }
}
