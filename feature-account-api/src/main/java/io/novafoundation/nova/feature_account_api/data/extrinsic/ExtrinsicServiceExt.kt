package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService.FeePaymentConfig
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.mapWithStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicWatchException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun Result<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>>.awaitInBlock(): Result<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>> =
    mapCatching { watchResult ->
        watchResult.awaitStatus<ExtrinsicStatus.InBlock>()
    }.onFailure {
        if (it is CancellationException) throw it
    }

suspend inline fun <reified T : ExtrinsicStatus> Flow<ExtrinsicWatchResult<*>>.awaitStatus(): ExtrinsicWatchResult<T> {
    val result = first { it.status is T || it.status.terminal }
    if (result.status is ExtrinsicStatus.Failure || result.status !is T) {
        throw ExtrinsicWatchException(result.status)
    }

    return result.mapWithStatus<T>()
}

inline fun <reified T : ExtrinsicStatus> Flow<ExtrinsicWatchResult<*>>.filterStatus(): Flow<ExtrinsicWatchResult<T>> {
    return filter { it.status is T }
        .map { it.mapWithStatus<T>() }
}

fun ExtrinsicService.Factory.createDefault(coroutineScope: CoroutineScope): ExtrinsicService {
    return create(FeePaymentConfig(coroutineScope))
}
