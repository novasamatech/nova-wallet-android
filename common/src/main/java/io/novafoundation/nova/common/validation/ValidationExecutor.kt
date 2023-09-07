package io.novafoundation.nova.common.validation

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.api.ValidationFailureUi
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

typealias ProgressConsumer = (Boolean) -> Unit

fun MutableLiveData<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

fun MutableStateFlow<Boolean>.progressConsumer(): ProgressConsumer = { value = it }

sealed class TransformedFailure {

    class Default(val titleAndMessage: TitleAndMessage) : TransformedFailure()

    class Custom(val dialogPayload: CustomDialogDisplayer.Payload) : TransformedFailure()
}

interface ValidationFlowActions<P> {

    fun resumeFlow(modifyPayload: ((P) -> P)? = null)

    fun revalidate(modifyPayload: ((P) -> P)? = null)
}

class ValidationExecutor : Validatable {

    suspend fun <P, S> requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        errorDisplayer: (Throwable) -> Unit,
        validationFailureTransformerCustom: (ValidationStatus.NotValid<S>, ValidationFlowActions<P>) -> TransformedFailure?,
        progressConsumer: ProgressConsumer? = null,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        scope: CoroutineScope,
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

                    val validationFlowActions = createFlowActions(
                        payload = payload,
                        autoFixPayload = autoFixPayload,
                        notValidStatus = it,
                        revalidate = { newPayload ->
                            scope.launch {
                                requireValid(
                                    validationSystem = validationSystem,
                                    payload = newPayload,
                                    errorDisplayer = errorDisplayer,
                                    validationFailureTransformerCustom = validationFailureTransformerCustom,
                                    progressConsumer = progressConsumer,
                                    autoFixPayload = autoFixPayload,
                                    block = block,
                                    scope = scope
                                )
                            }
                        },
                        successBlock = block
                    )

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

                        null -> null
                    }

                    eventPayload?.let {
                        validationFailureEvent.value = Event(eventPayload)
                    }
                }
            )
    }

    private fun <P, S> createFlowActions(
        payload: P,
        progressConsumer: ProgressConsumer? = null,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        notValidStatus: ValidationStatus.NotValid<S>,
        revalidate: (newPayload: P) -> Unit,
        successBlock: (newPayload: P) -> Unit,
    ) = object : ValidationFlowActions<P> {

        override fun resumeFlow(modifyPayload: ((P) -> P)?) {
            progressConsumer?.invoke(true)
            successBlock(transformPayload(modifyPayload))
        }

        override fun revalidate(modifyPayload: ((P) -> P)?) {
            progressConsumer?.invoke(true)
            revalidate(transformPayload(modifyPayload))
        }

        private fun transformPayload(modifyPayload: ((P) -> P)?): P {
            val payloadToAutoFix = modifyPayload?.invoke(payload) ?: payload

            // we do not remove autoFixPayload functionality for backward compatibility, with passing `modifiedPayload` becoming the preferred way
            return autoFixPayload(payloadToAutoFix, notValidStatus.reason)
        }
    }

    suspend fun <P, S> requireValid(
        validationSystem: ValidationSystem<P, S>,
        payload: P,
        errorDisplayer: (Throwable) -> Unit,
        validationFailureTransformerDefault: (S) -> TitleAndMessage,
        autoFixPayload: (original: P, failureStatus: S) -> P = { original, _ -> original },
        progressConsumer: ProgressConsumer? = null,
        scope: CoroutineScope,
        block: (P) -> Unit,
    ) = requireValid(
        validationSystem = validationSystem,
        payload = payload,
        errorDisplayer = errorDisplayer,
        validationFailureTransformerCustom = { it, _ -> TransformedFailure.Default(validationFailureTransformerDefault(it.reason)) },
        progressConsumer = progressConsumer,
        autoFixPayload = autoFixPayload,
        block = block,
        scope = scope
    )

    context (CoroutineScope)
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
        block = block,
        scope = this@CoroutineScope
    )

    override val validationFailureEvent = MutableLiveData<Event<ValidationFailureUi>>()
}


fun TitleAndMessage.asDefault() = TransformedFailure.Default(this)
