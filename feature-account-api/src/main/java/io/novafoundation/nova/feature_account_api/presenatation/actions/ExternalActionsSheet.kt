package io.novafoundation.nova.feature_account_api.presenatation.actions

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.runtime.ext.availableExplorersFor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias ExternalViewCallback = (Chain.Explorer, ExternalActions.Type) -> Unit
typealias CopyCallback = (String) -> Unit

open class ExternalActionsSheet(
    context: Context,
    protected val payload: ExternalActions.Payload,
    val onCopy: CopyCallback,
    val onViewExternal: ExternalViewCallback,
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.type.primaryValue)

        payload.copyLabelRes?.let {
            item(R.drawable.ic_copy, it) {
                onCopy(payload.type.primaryValue)
            }
        }

        payload.type.explorerTemplateExtractor?.let {
            payload.chain
                .availableExplorersFor(it)
                .forEach { explorer ->
                    val title = context.getString(R.string.transaction_details_view_explorer, explorer.name)

                    item(R.drawable.ic_globe_outline, title) {
                        onViewExternal(explorer, payload.type)
                    }
                }
        }
    }
}
