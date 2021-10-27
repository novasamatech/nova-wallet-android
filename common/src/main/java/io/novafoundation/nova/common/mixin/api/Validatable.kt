package io.novafoundation.nova.common.mixin.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.validation.ValidationStatus

class DefaultFailure(
    val level: ValidationStatus.NotValid.Level,
    val title: String,
    val message: String,
    val confirmWarning: Action
)

interface Validatable {
    val validationFailureEvent: LiveData<Event<DefaultFailure>>
}
