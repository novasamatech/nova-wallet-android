package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evm

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.ethereum.transaction.builder.contractCall
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class EvmAssetTransfers(
    private val evmTransactionService: EvmTransactionService,
    private val erc20Standard: Erc20Standard
) : AssetTransfers {

    override val validationSystem: AssetTransfersValidationSystem
        get() = ValidationSystem {
            // TODO validations
        }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return evmTransactionService.calculateFee(transfer.originChain.id) {
            transfer(transfer)
        }
    }

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        return evmTransactionService.transact(transfer.originChain.id) {
            transfer(transfer)
        }
    }

    private fun EvmTransactionBuilder.transfer(transfer: AssetTransfer) {
        val erc20 = transfer.originChainAsset.requireErc20()
        val recipient = transfer.originChain.accountIdOrDefault(transfer.recipient)

        contractCall(erc20.contractAddress, erc20Standard) {
            transfer(recipient = recipient, amount = transfer.amountInPlanks)
        }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return true
    }
}
