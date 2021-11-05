package io.novafoundation.nova.common.mixin.impl

import android.content.Context
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.warningDialog

fun BaseFragment<*>.observeValidations(
    viewModel: Validatable,
    dialogContext: Context = requireContext()
) {
    viewModel.validationFailureEvent.observeEvent {
        val level = it.level

        when {
            level >= DefaultFailureLevel.ERROR -> errorDialog(dialogContext) {
                setTitle(it.title)
                setMessage(it.message)
            }
            level >= DefaultFailureLevel.WARNING -> warningDialog(
                dialogContext,
                onConfirm = it.confirmWarning
            ) {
                setTitle(it.title)
                setMessage(it.message)
            }
        }
    }
}
