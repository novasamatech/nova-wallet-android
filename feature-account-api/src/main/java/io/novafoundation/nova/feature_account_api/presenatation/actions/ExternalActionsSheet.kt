package io.novafoundation.nova.feature_account_api.presenatation.actions

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.runtime.ext.availableExplorersFor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias ExternalViewCallback = (Chain.Explorer, ExternalActions.Type) -> Unit
typealias CopyCallback = (String) -> Unit

open class ExternalActionsSheet(
    context: Context,
    protected val payload: ExternalActions.Payload,
    val onCopy: CopyCallback,
    val onViewExternal: ExternalViewCallback,
) : FixedListBottomSheet(
    context,
    viewConfiguration = ViewConfiguration(
        layout = R.layout.bottom_sheet_external_actions,
        title = { externalActionsValue },
        container = { externalActionsContainer }
    )
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (payload.chainUi != null) {
            externalActionsChain.makeVisible()
            externalActionsChain.setChain(payload.chainUi)
        } else {
            externalActionsChain.makeGone()
        }

        if (payload.icon != null) {
            externalActionsIcon.makeVisible()
            externalActionsIcon.setImageDrawable(payload.icon)
        } else {
            externalActionsIcon.makeGone()
        }

        val primaryValue = payload.type.primaryValue

        setTitle(primaryValue)

        primaryValue?.let {
            textItem(R.drawable.ic_copy_outline, payload.copyLabelRes) {
                onCopy(primaryValue)
            }

            showExplorers()
        }
    }

    private fun showExplorers() {
        payload.chain
            .availableExplorersFor(payload.type.explorerTemplateExtractor)
            .forEach { explorer ->
                val title = context.getString(R.string.transaction_details_view_explorer, explorer.name)

                textItem(R.drawable.ic_browser_outline, title, showArrow = true) {
                    onViewExternal(explorer, payload.type)
                }
            }
    }
}
