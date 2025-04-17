package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.rebond

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfAwaitableAction
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_staking_impl.R

class ChooseRebondKindBottomSheet(
    context: Context,
    private val chooseOneOfAwaitableAction: ChooseOneOfAwaitableAction<RebondKind>,
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnCancelListener { chooseOneOfAwaitableAction.onCancel() }

        setTitle(R.string.staking_rebond)

        chooseOneOfAwaitableAction.payload.forEach { rebondKind ->
            textItem(R.drawable.ic_staking_outline, rebondKind.title) {
                chooseOneOfAwaitableAction.onSuccess(rebondKind)
            }
        }
    }
}
