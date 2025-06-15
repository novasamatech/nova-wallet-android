package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService.FeePaymentConfig
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.mapWithStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun Result<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>>.awaitInBlock(): Result<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>> =
    mapCatching { watchResult ->
        watchResult.filter { it.status is ExtrinsicStatus.InBlock }
            .map { it.mapWithStatus<ExtrinsicStatus.InBlock>() }
            .first()
    }

suspend inline fun <reified T : ExtrinsicStatus> Flow<ExtrinsicWatchResult<*>>.awaitStatus(): ExtrinsicWatchResult<T> {
    return filterStatus<T>().first()
}

inline fun <reified T : ExtrinsicStatus> Flow<ExtrinsicWatchResult<*>>.filterStatus(): Flow<ExtrinsicWatchResult<T>> {
    return filter { it.status is T }
        .map { it.mapWithStatus<T>() }
}

fun ExtrinsicService.Factory.createDefault(coroutineScope: CoroutineScope): ExtrinsicService {
    return create(FeePaymentConfig(coroutineScope))
}
