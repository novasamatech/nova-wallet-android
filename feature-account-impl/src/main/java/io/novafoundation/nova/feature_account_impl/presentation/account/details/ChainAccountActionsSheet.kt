package io.novafoundation.nova.feature_account_impl.presentation.account.details

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
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
    private val onChange: (inChain: Chain) -> Unit,
    private val onExport: (inChain: Chain) -> Unit,
) : ExternalActionsSheet(context, payload, onCopy, onViewExternal) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        item(R.drawable.ic_staking_operations, R.string.accounts_change_chain_secrets) {
            onChange(payload.chain)
        }

        if (payload.type !is ExternalActions.Type.None) {
            item(R.drawable.ic_share_arrow_white_24, R.string.account_export) {
                onExport(payload.chain)
            }
        }
    }
}
