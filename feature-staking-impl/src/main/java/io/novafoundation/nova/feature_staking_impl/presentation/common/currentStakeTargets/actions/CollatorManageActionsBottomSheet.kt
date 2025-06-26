package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.actions

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_staking_impl.R

class CollatorManageActionsBottomSheet(
    context: Context,
    private val itemSelected: (ManageCurrentStakeTargetsAction) -> Unit,
    onCancel: (() -> Unit)? = null,
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context), onCancel) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.parachain_staking_manage_collators)

        ManageCurrentStakeTargetsAction.values().forEach {
            item(it)
        }
    }

    private fun item(action: ManageCurrentStakeTargetsAction) = textItem(action.iconRes, action.titleRes) {
        itemSelected(action)
    }
}
