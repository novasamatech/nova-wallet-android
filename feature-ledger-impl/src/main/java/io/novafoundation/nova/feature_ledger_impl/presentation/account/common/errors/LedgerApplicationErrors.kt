package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

fun <V> V.handleLedgerError(
    reason: Throwable,
    chain: Deferred<Chain>,
    resourceManager: ResourceManager,
    retry: () -> Unit
) where V : BaseViewModel, V : Retriable {
    reason.printStackTrace()

    launch {
        when (reason) {
            is CancellationException -> {
                // do nothing on coroutines cancellation
            }
            is SubstrateLedgerApplicationError.Response -> handleSubstrateApplicationError(reason.response, chain(), resourceManager, retry)
            else -> showError(
                title = resourceManager.getString(R.string.ledger_error_general_title),
                text = resourceManager.getString(R.string.ledger_error_general_message)
            )
        }
    }
}

private fun Retriable.handleSubstrateApplicationError(
    reason: LedgerApplicationResponse,
    chain: Chain,
    resourceManager: ResourceManager,
    retry: () -> Unit
) {
    val errorTitle: String
    val errorMessage: String

    when (reason) {
        LedgerApplicationResponse.appNotOpen, LedgerApplicationResponse.wrongAppOpen -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_app_not_launched_title, chain.name)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_not_launched_message, chain.name)
        }

        LedgerApplicationResponse.transactionRejected -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_app_cancelled_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_app_cancelled_message)
        }

        else -> {
            errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
            errorMessage = resourceManager.getString(R.string.ledger_error_general_message)
        }
    }

    retryEvent.value = RetryPayload(
        title = errorTitle,
        message = errorMessage,
        onRetry = retry
    ).event()
}
