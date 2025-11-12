package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmNative

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.TransactionExecution
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.checkForFeeChanges
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.recipientIsNotSystemAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.recipientCanAcceptTransfer
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.CoroutineScope

// native coin transfer has a fixed fee
private val NATIVE_COIN_TRANSFER_GAS_LIMIT = 21_000.toBigInteger()

class EvmNativeAssetTransfers(
    private val evmTransactionService: EvmTransactionService,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetTransfers {

    override fun getValidationSystem(coroutineScope: CoroutineScope) = ValidationSystem {
        validAddress()
        recipientIsNotSystemAccount()

        positiveAmount()

        sufficientBalanceInUsedAsset()
        sufficientTransferableBalanceToPayOriginFee()

        recipientCanAcceptTransfer(assetSourceRegistry)

        checkForFeeChanges(assetSourceRegistry, coroutineScope)
    }

    override suspend fun calculateFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): Fee {
        return evmTransactionService.calculateFee(
            transfer.originChain.id,
            fallbackGasLimit = NATIVE_COIN_TRANSFER_GAS_LIMIT,
            origin = transfer.sender.intoOrigin()
        ) {
            nativeTransfer(transfer)
        }
    }

    override suspend fun performTransfer(transfer: WeightedAssetTransfer, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission> {
        return evmTransactionService.transact(
            chainId = transfer.originChain.id,
            fallbackGasLimit = NATIVE_COIN_TRANSFER_GAS_LIMIT,
            presetFee = transfer.fee.submissionFee,
            origin = transfer.sender.intoOrigin()
        ) {
            nativeTransfer(transfer)
        }
    }

    override suspend fun performTransferAndAwaitExecution(transfer: WeightedAssetTransfer, coroutineScope: CoroutineScope): Result<TransactionExecution> {
        return evmTransactionService.transactAndAwaitExecution(
            chainId = transfer.originChain.id,
            fallbackGasLimit = NATIVE_COIN_TRANSFER_GAS_LIMIT,
            presetFee = transfer.fee.submissionFee,
            origin = transfer.sender.intoOrigin()
        ) {
            nativeTransfer(transfer)
        }.map { TransactionExecution.Ethereum(it) }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun parseTransfer(call: GenericCall.Instance, chain: Chain): TransferParsedFromCall? {
        return null
    }

    private fun EvmTransactionBuilder.nativeTransfer(transfer: AssetTransfer) {
        val recipient = transfer.originChain.accountIdOrDefault(transfer.recipient)

        nativeTransfer(transfer.amountInPlanks, recipient)
    }
}
