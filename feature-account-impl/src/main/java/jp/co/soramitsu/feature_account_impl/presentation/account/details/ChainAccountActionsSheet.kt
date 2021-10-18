package jp.co.soramitsu.feature_account_impl.presentation.account.details

import android.content.Context
import android.os.Bundle
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.view.bottomSheet.list.fixed.item
import jp.co.soramitsu.feature_account_api.presenatation.actions.CopyCallback
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActionsSheet
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalViewCallback
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

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

        item(R.drawable.ic_edit, R.string.account_chain_change) {
            onChange(payload.chain)
        }

        if (payload.type !is ExternalActions.Type.None) {
            item(R.drawable.ic_share_arrow_white_24, R.string.account_export) {
                onExport(payload.chain)
            }
        }
    }
}
