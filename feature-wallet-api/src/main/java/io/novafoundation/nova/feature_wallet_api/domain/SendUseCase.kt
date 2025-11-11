package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.TransactionExecution
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import kotlinx.coroutines.CoroutineScope

interface SendUseCase {

    suspend fun performOnChainTransfer(transfer: WeightedAssetTransfer, fee: SubmissionFee, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission>

    suspend fun performOnChainTransferAndAwaitExecution(
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<TransactionExecution>
}
