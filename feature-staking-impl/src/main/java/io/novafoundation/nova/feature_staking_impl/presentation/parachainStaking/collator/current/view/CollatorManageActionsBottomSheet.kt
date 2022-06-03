package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.model.ManageCollatorsAction

class CollatorManageActionsBottomSheet(
    context: Context,
    private val itemSelected: (ManageCollatorsAction) -> Unit,
    onCancel: (() -> Unit)? = null,
    ): FixedListBottomSheet(context, onCancel) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.parachain_staking_manage_collators)
        setTitleDividerVisible(false)

        ManageCollatorsAction.values().forEach {
            item(it)
        }
    }

    private fun item(
        action: ManageCollatorsAction,
    ) = item(action.iconRes, action.titleRes) {
        itemSelected(action)
    }
}
