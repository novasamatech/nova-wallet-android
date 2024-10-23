package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService.FeePaymentConfig
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

suspend fun Result<Flow<ExtrinsicStatus>>.awaitInBlock(): Result<ExtrinsicStatus.InBlock> = mapCatching {
    it.filterIsInstance<ExtrinsicStatus.InBlock>().first()
}

fun ExtrinsicService.Factory.createDefault(coroutineScope: CoroutineScope): ExtrinsicService {
    return create(FeePaymentConfig(coroutineScope))
}
