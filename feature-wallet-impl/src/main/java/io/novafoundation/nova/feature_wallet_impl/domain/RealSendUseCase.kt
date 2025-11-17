package io.novafoundation.nova.feature_wallet_impl.domain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.signer.isImmediate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.TransactionExecution
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.isCrossChain
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import java.security.InvalidParameterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealSendUseCase(
    private val walletRepository: WalletRepository,
    private val assetSourceRegistry: AssetSourceRegistry
) : SendUseCase {

    override suspend fun performOnChainTransfer(
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<ExtrinsicSubmission> = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) throw InvalidParameterException("Cross chain transfers are not supported")
        getAssetTransfers(transfer).performTransfer(transfer, coroutineScope)
            .onSuccess { submission ->
                // Only add pending history items for calls that are executed immediately
                if (submission.callExecutionType.isImmediate()) {
                    // Insert used fee regardless of who paid it
                    walletRepository.insertPendingTransfer(submission.hash, transfer, fee)
                }
            }
    }

    override suspend fun performOnChainTransferAndAwaitExecution(
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<TransactionExecution> = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) throw InvalidParameterException("Cross chain transfers are not supported")
        getAssetTransfers(transfer).performTransferAndAwaitExecution(transfer, coroutineScope)
    }

    private fun getAssetTransfers(transfer: AssetTransfer) = assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers
}
