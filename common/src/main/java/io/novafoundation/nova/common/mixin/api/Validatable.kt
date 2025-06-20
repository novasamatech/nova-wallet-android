package io.novafoundation.nova.common.mixin.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.validation.ValidationStatus

sealed class ValidationFailureUi {

    class Default(
        val level: ValidationStatus.NotValid.Level,
        val title: String,
        val message: CharSequence?,
        val confirmWarning: Action,
    ) : ValidationFailureUi()

    class Custom(val payload: CustomDialogDisplayer.Payload) : ValidationFailureUi()
}

interface Validatable {
    val validationFailureEvent: LiveData<Event<ValidationFailureUi>>
}
