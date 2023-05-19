package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmNative

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.recipientCanAcceptTransfer
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

// native coin transfer has a fixed fee
private val NATIVE_COIN_TRANSFER_GAS_LIMIT = 21_000.toBigInteger()

class EvmNativeAssetTransfers(
    private val evmTransactionService: EvmTransactionService,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem = ValidationSystem {
        validAddress()

        positiveAmount()

        sufficientBalanceInUsedAsset()
        sufficientTransferableBalanceToPayOriginFee()

        recipientCanAcceptTransfer(assetSourceRegistry)
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return evmTransactionService.calculateFee(transfer.originChain.id, fallbackGasLimit = NATIVE_COIN_TRANSFER_GAS_LIMIT) {
            nativeTransfer(transfer)
        }
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        return evmTransactionService.transact(transfer.originChain.id, fallbackGasLimit = NATIVE_COIN_TRANSFER_GAS_LIMIT) {
            nativeTransfer(transfer)
        }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return true
    }

    private fun EvmTransactionBuilder.nativeTransfer(transfer: AssetTransfer) {
        val recipient = transfer.originChain.accountIdOrDefault(transfer.recipient)

        nativeTransfer(transfer.amountInPlanks, recipient)
    }
}
