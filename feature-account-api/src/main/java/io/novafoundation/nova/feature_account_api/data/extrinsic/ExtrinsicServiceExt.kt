package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

suspend fun Result<Flow<ExtrinsicStatus>>.awaitInBlock(): Result<ExtrinsicStatus.InBlock> = mapCatching {
    it.filterIsInstance<ExtrinsicStatus.InBlock>().first()
}

fun ExtrinsicService.SubmissionOptions.selectedCommissionAsset(chain: Chain): Chain.Asset {
    return when (feePaymentCurrency) {
        is FeePaymentCurrency.Asset -> feePaymentCurrency.asset
        FeePaymentCurrency.Native -> chain.commissionAsset
    }
}
