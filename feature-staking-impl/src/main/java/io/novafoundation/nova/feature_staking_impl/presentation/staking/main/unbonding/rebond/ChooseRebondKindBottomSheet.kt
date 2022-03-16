package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding.rebond

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.feature_staking_impl.R

class ChooseRebondKindBottomSheet(
    context: Context,
    private val actionListener: (RebondKind) -> Unit,
    private val onCancel: () -> Unit
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnCancelListener { onCancel() }

        setTitle(R.string.staking_rebond)

        item(R.drawable.ic_staking_outline, R.string.staking_rebond_all) {
            actionListener(RebondKind.ALL)
        }

        item(R.drawable.ic_staking_outline, R.string.staking_rebond_last) {
            actionListener(RebondKind.LAST)
        }

        item(R.drawable.ic_staking_outline, R.string.staking_rebond_custom) {
            actionListener(RebondKind.CUSTOM)
        }
    }
}
