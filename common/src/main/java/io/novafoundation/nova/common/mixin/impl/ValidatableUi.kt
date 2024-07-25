package io.novafoundation.nova.common.mixin.impl

import android.content.Context
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.api.ValidationFailureUi
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.warningDialog

fun BaseFragmentMixin<*>.observeValidations(
    viewModel: Validatable,
    dialogContext: Context = providedContext
) {
    viewModel.validationFailureEvent.observeEvent {
        when (it) {
            is ValidationFailureUi.Default -> {
                val level = it.level

                when {
                    level >= DefaultFailureLevel.ERROR -> errorDialog(dialogContext) {
                        setTitle(it.title)
                        setMessage(it.message)
                    }
                    level >= DefaultFailureLevel.WARNING -> warningDialog(
                        context = dialogContext,
                        onPositiveClick = it.confirmWarning
                    ) {
                        setTitle(it.title)
                        setMessage(it.message)
                    }
                }
            }
            is ValidationFailureUi.Custom -> displayDialogFor(it.payload)
        }
    }
}
