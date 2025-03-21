package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Show.Error.RecoverableError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter.MessageKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun <V> V.handleLedgerError(
    reason: Throwable,
    device: LedgerDevice,
    commandFormatter: MessageCommandFormatter,
    onRetry: () -> Unit
) where V : BaseViewModel, V : LedgerMessageCommands, V : Browserable.Presentation {
    reason.printStackTrace()

    launch {
        when (reason) {
            is CancellationException -> {
                // do nothing on coroutines cancellation
            }

            is SubstrateLedgerApplicationError.Response -> {
                ledgerMessageCommands.value = commandFormatter.substrateApplicationError(
                    reason = reason.response,
                    device = device,
                    onCancel = ::hide,
                    onRetry = onRetry
                ).event()
            }

            else -> {
                ledgerMessageCommands.value = commandFormatter.unknownError(
                    device = device,
                    onRetry = onRetry,
                    onCancel = ::hide
                ).event()
            }
        }
    }
}

private fun LedgerMessageCommands.hide() {
    ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
}
