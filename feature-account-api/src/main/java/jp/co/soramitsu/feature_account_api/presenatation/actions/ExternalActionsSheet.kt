package jp.co.soramitsu.feature_account_api.presenatation.actions

import android.content.Context
import android.os.Bundle
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import jp.co.soramitsu.common.view.bottomSheet.list.fixed.item
import jp.co.soramitsu.runtime.ext.availableExplorersFor
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

typealias ExternalViewCallback = (Chain.Explorer, ExternalActions.Type) -> Unit
typealias CopyCallback = (String) -> Unit

open class ExternalActionsSheet(
    context: Context,
    private val payload: ExternalActions.Payload,
    val onCopy: CopyCallback,
    val onViewExternal: ExternalViewCallback,
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.type.primaryValue)

        item(R.drawable.ic_copy_24, payload.copyLabelRes) {
            onCopy(payload.type.primaryValue)
        }

        payload.chain
            .availableExplorersFor(Chain.Explorer::account)
            .forEach { explorer ->
                val title = context.getString(R.string.transaction_details_view_explorer, explorer.name)

                item(R.drawable.ic_globe_24, title) {
                    onViewExternal(explorer, payload.type)
                }
            }
    }
}
