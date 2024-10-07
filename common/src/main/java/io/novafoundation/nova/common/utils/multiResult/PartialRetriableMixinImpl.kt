package io.novafoundation.nova.common.utils.multiResult

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.errors.shouldIgnore
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.validation.ProgressConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RealPartialRetriableMixinFactory(
    private val resourceManager: ResourceManager
) : PartialRetriableMixin.Factory {

    override fun create(scope: CoroutineScope): PartialRetriableMixin.Presentation {
        return PartialRetriableMixinImpl(resourceManager, scope)
    }
}

private class PartialRetriableMixinImpl(
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope
) : PartialRetriableMixin.Presentation, CoroutineScope by coroutineScope {

    override suspend fun <T> handleMultiResult(
        multiResult: RetriableMultiResult<T>,
        onSuccess: suspend (List<T>) -> Unit,
        progressConsumer: ProgressConsumer?,
        onRetryCancelled: () -> Unit
    ) {
        multiResult
            .onAnyFailure { submissionFailed(it, onSuccess, progressConsumer, onRetryCancelled) }
            .onFullSuccess(onSuccess)

        progressConsumer?.invoke(false)
    }

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    private fun <T> submissionFailed(
        failure: RetriableMultiResult.RetriableFailure<T>,
        onSuccess: suspend (List<T>) -> Unit,
        progressConsumer: ProgressConsumer?,
        onRetryCancelled: () -> Unit,
    ) {
        if (shouldIgnore(failure.error)) return

        val onRetry = { retrySubmission(failure, onSuccess, progressConsumer, onRetryCancelled) }

        retryEvent.value = RetryPayload(
            title = resourceManager.getString(R.string.common_some_tx_failed_title),
            message = resourceManager.getString(R.string.common_some_tx_failed_message),
            onRetry = onRetry,
            onCancel = onRetryCancelled
        ).event()
    }

    private fun <T> retrySubmission(
        failure: RetriableMultiResult.RetriableFailure<T>,
        onSuccess: suspend (List<T>) -> Unit,
        progressConsumer: ProgressConsumer?,
        onRetryCancelled: () -> Unit
    ) {
        progressConsumer?.invoke(true)

        launch {
            val newResult = withContext(Dispatchers.Default) { failure.retry() }
            handleMultiResult(newResult, onSuccess, progressConsumer, onRetryCancelled)
        }
    }
}
