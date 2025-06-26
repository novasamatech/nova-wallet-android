package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_impl.R

fun BaseFragment<*, *>.setupCloseAllDappTabsDialogue(mixin: ConfirmationAwaitable<Unit>) {
    mixin.awaitableActionLiveData.observeEvent { event ->
        warningDialog(
            context = providedContext,
            onPositiveClick = { event.onSuccess(Unit) },
            positiveTextRes = R.string.browser_tabs_close_all,
            negativeTextRes = R.string.common_cancel,
            onNegativeClick = { event.onCancel() },
            styleRes = R.style.AccentNegativeAlertDialogTheme_Reversed
        ) {
            setTitle(R.string.close_dapp_tabs_title)

            setMessage(R.string.close_dapp_tabs_message)
        }
    }
}
