package io.novafoundation.nova.common.validation

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.api.ValidationFailureUi
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow

typealias ProgressConsumer = (Boolean) -> Unit

fun MutableLiveData<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

fun MutableStateFlow<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

sealed class TransformedFailure {

    class Default(val titleAndMessage: TitleAndMessage) : TransformedFailure()

    class Custom(val dialogPayload: CustomDialogDisplayer.Payload) : TransformedFailure()
}

interface ValidationFlowActions {

    fun resumeFlow()
}

class ValidationExecutor : Validatable {

    suspend fun <P, S> requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        errorDisplayer: (Throwable) -> Unit,
        validationFailureTransformerCustom: (ValidationStatus.NotValid<S>, ValidationFlowActions) -> TransformedFailure,
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

                    val validationFlowActions = createFlowActions(payload, progressConsumer, autoFixPayload, block, it)

                    val eventPayload = when (val transformedFailure = validationFailureTransformerCustom(it, validationFlowActions)) {
                        is TransformedFailure.Custom -> ValidationFailureUi.Custom(transformedFailure.dialogPayload)

                        is TransformedFailure.Default -> {
                            val (title, message) = transformedFailure.titleAndMessage

                            ValidationFailureUi.Default(
                                level = it.level,
                                title = title,
                                message = message,
                                confirmWarning = validationFlowActions::resumeFlow
                            )
                        }
                    }

                    validationFailureEvent.value = Event(eventPayload)
                }
            )
    }

    private fun <P, S> createFlowActions(
        payload: P,
        progressConsumer: ProgressConsumer? = null,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        block: (P) -> Unit,
        notValidStatus: ValidationStatus.NotValid<S>,
    ) = object : ValidationFlowActions {

        override fun resumeFlow() {
            progressConsumer?.invoke(true)

            val transformedPayload = autoFixPayload(payload, notValidStatus.reason)

            block(transformedPayload)
        }
    }

    suspend fun <P, S> requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        errorDisplayer: (Throwable) -> Unit,
        validationFailureTransformerDefault: (S) -> TitleAndMessage,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        progressConsumer: ProgressConsumer? = null,
        block: (P) -> Unit,
    ) = requireValid(
        validationSystem = validationSystem,
        payload = payload,
        errorDisplayer = errorDisplayer,
        validationFailureTransformerCustom = { it, _ -> TransformedFailure.Default(validationFailureTransformerDefault(it.reason)) },
        progressConsumer = progressConsumer,
        autoFixPayload = autoFixPayload,
        block = block
    )

    override val validationFailureEvent = MutableLiveData<Event<ValidationFailureUi>>()
}
