package io.novafoundation.nova.feature_wallet_impl.presentation.send

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.dialog.DialogClickHandler
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel
import io.novafoundation.nova.feature_wallet_impl.R

interface TransferValidityChecks {
    val showTransferWarning: LiveData<Event<TransferValidityLevel.Warning.Status>>

    val showTransferError: LiveData<Event<TransferValidityLevel.Error.Status>>

    interface Presentation : TransferValidityChecks {
        fun showTransferWarning(warning: TransferValidityLevel.Warning.Status)

        fun showTransferError(error: TransferValidityLevel.Error.Status)
    }
}

class TransferValidityChecksProvider : TransferValidityChecks.Presentation {
    override fun showTransferWarning(warning: TransferValidityLevel.Warning.Status) {
        showTransferWarning.value = Event(warning)
    }

    override fun showTransferError(error: TransferValidityLevel.Error.Status) {
        showTransferError.value = Event(error)
    }

    override val showTransferWarning = MutableLiveData<Event<TransferValidityLevel.Warning.Status>>()

    override val showTransferError = MutableLiveData<Event<TransferValidityLevel.Error.Status>>()
}

fun <T> BaseFragment<T>.observeTransferChecks(
    viewModel: T,
    warningConfirmed: DialogClickHandler,
    errorConfirmed: DialogClickHandler? = null
) where T : BaseViewModel, T : TransferValidityChecks {
    viewModel.showTransferWarning.observeEvent {
        showTransferWarning(it, warningConfirmed)
    }

    viewModel.showTransferError.observeEvent {
        showTransferError(it, errorConfirmed)
    }
}

private fun BaseFragment<*>.showTransferError(
    status: TransferValidityLevel.Error.Status,
    errorConfirmed: DialogClickHandler?
) {
    val (titleRes, messageRes) = when (status) {
        TransferValidityLevel.Error.Status.NotEnoughFunds -> R.string.common_error_general_title to R.string.choose_amount_error_too_big
        TransferValidityLevel.Error.Status.DeadRecipient -> R.string.common_amount_low to R.string.wallet_send_dead_recipient_message
    }

    errorDialog(requireContext(), errorConfirmed) {
        setTitle(titleRes)
        setMessage(messageRes)
    }
}

private fun BaseFragment<*>.showTransferWarning(
    status: TransferValidityLevel.Warning.Status,
    warningConfirmed: DialogClickHandler
) {
    val (title, message) = when (status) {
        TransferValidityLevel.Warning.Status.WillRemoveAccount -> {
            R.string.wallet_send_existential_warning_title to R.string.wallet_send_existential_warning_message_v2_2_0
        }
    }

    warningDialog(requireContext(), warningConfirmed) {
        setTitle(title)
        setMessage(message)
    }
}
