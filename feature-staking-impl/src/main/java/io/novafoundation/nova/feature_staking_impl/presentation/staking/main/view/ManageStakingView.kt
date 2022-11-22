package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction

typealias ManageStakeActionClickListener = (ManageStakeAction) -> Unit

class ManageStakingView @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle),
    WithContextExtensions by WithContextExtensions(context) {

    private var listener: ManageStakeActionClickListener? = null

    init {
        orientation = VERTICAL
        updatePadding(top = 8.dp, bottom = 8.dp)

        background = context.getBlockDrawable()
        clipToOutline = true
    }

    fun setAvailableActions(actions: Collection<ManageStakeAction>) {
        removeAllViews()

        actions.forEach { addViewFor(it) }
    }

    private fun addViewFor(action: ManageStakeAction) {
        val view = ManageStakingActionView(context).apply {
            setLabel(action.label)
            setIconRes(action.iconRes)
            setBadge(action.badge)

            setOnClickListener { listener?.invoke(action) }
        }

        addView(view)
    }

    fun onManageStakeActionClicked(listener: ManageStakeActionClickListener) {
        this.listener = listener
    }
}
