package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.model.ManageCollatorsAction

class CollatorManageActionsBottomSheet(
    context: Context,
    private val itemSelected: (ManageCollatorsAction) -> Unit,
    onCancel: (() -> Unit)? = null,
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context), onCancel) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.parachain_staking_manage_collators)

        ManageCollatorsAction.values().forEach {
            item(it)
        }
    }

    private fun item(
        action: ManageCollatorsAction,
    ) = textItem(action.iconRes, action.titleRes) {
        itemSelected(action)
    }
}
