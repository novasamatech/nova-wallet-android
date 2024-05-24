package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Show.Error.RecoverableError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun <V> V.handleLedgerError(
    reason: Throwable,
    messageFormatter: LedgerMessageFormatter,
    resourceManager: ResourceManager,
    retry: () -> Unit
) where V : BaseViewModel, V : LedgerMessageCommands {
    reason.printStackTrace()

    launch {
        when (reason) {
            is CancellationException -> {
                // do nothing on coroutines cancellation
            }
            is SubstrateLedgerApplicationError.Response -> handleSubstrateApplicationError(reason.response, messageFormatter.appName(), resourceManager, retry)
            else -> handleUnknownError(reason, resourceManager, retry)
        }
    }
}

private fun LedgerMessageCommands.handleUnknownError(
    reason: Throwable,
    resourceManager: ResourceManager,
    retry: () -> Unit,
) {
    ledgerMessageCommands.value = RetryCommand(
        title = resourceManager.getString(R.string.ledger_error_general_title),
        subtitle = resourceManager.getString(R.string.ledger_error_general_message),
        retry = retry,
    )
}

private fun LedgerMessageCommands.handleSubstrateApplicationError(
    reason: LedgerApplicationResponse,
    ledgerAppName: String,
    resourceManager: ResourceManager,
    retry: () -> Unit
) {
    val errorTitle: String
    val errorMessage: String

    when (reason) {
        LedgerApplicationResponse.APP_NOT_OPEN, LedgerApplicationResponse.WRONG_APP_OPEN -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_app_not_launched_title, ledgerAppName)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_not_launched_message, ledgerAppName)
        }

        LedgerApplicationResponse.TRANSACTION_REJECTED -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_app_cancelled_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_cancelled_message)
        }

        else -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_general_message)
        }
    }

    ledgerMessageCommands.value = RetryCommand(retry, errorTitle, errorMessage)
}

private fun LedgerMessageCommands.RetryCommand(
    retry: () -> Unit,
    title: String,
    subtitle: String
): Event<LedgerMessageCommand> = RecoverableError(
    title = title,
    subtitle = subtitle,
    onCancel = ::hide,
    onRetry = retry
).event()

private fun LedgerMessageCommands.hide() {
    ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
}
