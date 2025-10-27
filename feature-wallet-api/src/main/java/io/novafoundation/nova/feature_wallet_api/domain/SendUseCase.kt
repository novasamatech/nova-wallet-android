package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import kotlinx.coroutines.CoroutineScope

interface SendUseCase {
    suspend fun performTransfer(transfer: WeightedAssetTransfer, fee: SubmissionFee, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission>
}
