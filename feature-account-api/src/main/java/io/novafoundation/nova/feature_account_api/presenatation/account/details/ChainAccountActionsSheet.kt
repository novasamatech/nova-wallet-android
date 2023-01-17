package io.novafoundation.nova.feature_account_api.presenatation.account.details

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_api.presenatation.actions.CopyCallback
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActionsSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalViewCallback
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainAccountActionsSheet(
    context: Context,
    payload: ExternalActions.Payload,
    onCopy: CopyCallback,
    onViewExternal: ExternalViewCallback,
    private val availableAccountActions: Set<AccountAction>,
    private val onChange: (inChain: Chain) -> Unit,
    private val onExport: (inChain: Chain) -> Unit,
) : ExternalActionsSheet(context, payload, onCopy, onViewExternal) {

    enum class AccountAction {
        EXPORT,
        CHANGE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAvailableAccountActions()
    }

    private fun showAvailableAccountActions() {
        availableAccountActions.forEach {
            when (it) {
                AccountAction.EXPORT -> maybeShowExport()
                AccountAction.CHANGE -> maybeShowChange()
            }
        }
    }

    private fun maybeShowExport() {
        accountAddress()?.let {
            textItem(R.drawable.ic_share_outline, R.string.account_export, showArrow = true) {
                onExport.invoke(payload.chain)
            }
        }
    }

    private fun maybeShowChange() {
        val address = accountAddress()

        if (address != null) {
            changeAccountItem(R.string.accounts_change_chain_secrets)
        } else {
            changeAccountItem(R.string.account_add_account)
        }
    }

    private fun changeAccountItem(@StringRes labelRes: Int) {
        textItem(R.drawable.ic_staking_operations, labelRes, showArrow = true) {
            onChange.invoke(payload.chain)
        }
    }

    private fun accountAddress() = payload.type.castOrNull<ExternalActions.Type.Address>()?.address
}
