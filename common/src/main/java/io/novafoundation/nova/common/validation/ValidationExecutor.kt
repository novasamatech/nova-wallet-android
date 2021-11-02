package io.novafoundation.nova.common.validation

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.DefaultFailure
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow

typealias ProgressConsumer = (Boolean) -> Unit

fun MutableLiveData<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

fun MutableStateFlow<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

class ValidationExecutor: Validatable {

    suspend fun <P, S> requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        errorDisplayer: (Throwable) -> Unit,
        validationFailureTransformer: (S) -> TitleAndMessage,
        progressConsumer: ProgressConsumer? = null,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        block: (P) -> Unit,
    ) {
        progressConsumer?.invoke(true)

        validationSystem.validate(payload)
            .unwrap(
                onValid = { block(payload) },
                onFailure = {
                    progressConsumer?.invoke(false)

                    errorDisplayer(it)
                },
                onInvalid = {
                    progressConsumer?.invoke(false)

                    val (title, message) = validationFailureTransformer(it.reason)

                    validationFailureEvent.value = Event(
                        DefaultFailure(
                            level = it.level,
                            title = title,
                            message = message,
                            confirmWarning = {
                                progressConsumer?.invoke(true)

                                val transformedPayload = autoFixPayload(payload, it.reason)

                                block(transformedPayload)
                            }
                        )
                    )
                }
            )
    }

    override val validationFailureEvent = MutableLiveData<Event<DefaultFailure>>()
}
