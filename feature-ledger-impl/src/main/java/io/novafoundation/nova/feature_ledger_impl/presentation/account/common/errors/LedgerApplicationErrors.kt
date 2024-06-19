package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Show.Error.RecoverableError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter.MessageKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun <V> V.handleLedgerError(
    reason: Throwable,
    messageFormatter: LedgerMessageFormatter,
    resourceManager: ResourceManager,
    retry: () -> Unit
) where V : BaseViewModel, V : LedgerMessageCommands, V : Browserable.Presentation {
    reason.printStackTrace()

    launch {
        when (reason) {
            is CancellationException -> {
                // do nothing on coroutines cancellation
            }

            is SubstrateLedgerApplicationError.Response -> handleSubstrateApplicationError(reason.response, messageFormatter, resourceManager, retry)
            else -> handleUnknownError(reason, resourceManager, messageFormatter, retry)
        }
    }
}

private suspend fun <V> V.handleUnknownError(
    reason: Throwable,
    resourceManager: ResourceManager,
    ledgerMessageFormatter: LedgerMessageFormatter,
    retry: () -> Unit,
) where V : LedgerMessageCommands, V : Browserable.Presentation {
    ledgerMessageCommands.value = RetryCommand(
        title = resourceManager.getString(R.string.ledger_error_general_title),
        subtitle = resourceManager.getString(R.string.ledger_error_general_message),
        alertModel = ledgerMessageFormatter.alertForKind(MessageKind.OTHER),
        retry = retry,
    )
}

private suspend fun <V> V.handleSubstrateApplicationError(
    reason: LedgerApplicationResponse,
    ledgerMessageFormatter: LedgerMessageFormatter,
    resourceManager: ResourceManager,
    retry: () -> Unit
) where V : LedgerMessageCommands, V : Browserable.Presentation {
    val errorTitle: String
    val errorMessage: String
    val alert: AlertModel?

    when (reason) {
        LedgerApplicationResponse.APP_NOT_OPEN, LedgerApplicationResponse.WRONG_APP_OPEN -> {
            val appName = ledgerMessageFormatter.appName()

            errorTitle = resourceManager.getString(R.string.ledger_error_app_not_launched_title, appName)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_not_launched_message, appName)
            alert = ledgerMessageFormatter.alertForKind(MessageKind.APP_NOT_OPEN)
        }

        LedgerApplicationResponse.TRANSACTION_REJECTED -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_app_cancelled_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_cancelled_message)
            alert = ledgerMessageFormatter.alertForKind(MessageKind.OTHER)
        }

        else -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_general_message)
            alert = ledgerMessageFormatter.alertForKind(MessageKind.OTHER)
        }
    }

    ledgerMessageCommands.value = RetryCommand(retry = retry, title = errorTitle, subtitle = errorMessage, alertModel = alert)
}

private fun LedgerMessageCommands.RetryCommand(
    retry: () -> Unit,
    title: String,
    subtitle: String,
    alertModel: AlertModel?
): Event<LedgerMessageCommand> = RecoverableError(
    title = title,
    subtitle = subtitle,
    alert = alertModel,
    onCancel = ::hide,
    onRetry = retry
).event()

private fun LedgerMessageCommands.hide() {
    ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
}
