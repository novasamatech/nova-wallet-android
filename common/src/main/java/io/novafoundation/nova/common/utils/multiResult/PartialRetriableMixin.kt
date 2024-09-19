package io.novafoundation.nova.common.utils.multiResult

import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.validation.ProgressConsumer
import kotlinx.coroutines.CoroutineScope

interface PartialRetriableMixin : Retriable {

    interface Factory {

        fun create(scope: CoroutineScope): Presentation
    }

    interface Presentation : PartialRetriableMixin {

        suspend fun <T> handleMultiResult(
            multiResult: RetriableMultiResult<T>,
            onSuccess: suspend (List<T>) -> Unit,
            progressConsumer: ProgressConsumer? = null,
            onRetryCancelled: () -> Unit
        )
    }
}
