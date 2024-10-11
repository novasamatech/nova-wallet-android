package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R

class UnbondingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val unbondingsAdapter = UnbondingsAdapter()

    init {
        View.inflate(context, R.layout.view_unbondings, this)

        background = context.getRoundedCornerDrawable(R.color.block_background)

        unbondingsList.adapter = unbondingsAdapter
    }

    fun onRedeemClicked(action: () -> Unit) {
        unbondingRedeem.setOnClickListener { action() }
    }

    fun onCancelClicked(action: () -> Unit) {
        unbondingCancel.setOnClickListener { action() }
    }

    fun setState(state: UnbondingState) {
        when (state) {
            UnbondingState.Empty -> makeGone()
            is UnbondingState.HaveUnbondings -> {
                makeVisible()

                unbondingsAdapter.submitList(state.unbondings)

                unbondingRedeem.isEnabled = state.redeemEnabled
                unbondingCancel.setState(state.cancelState)
            }
        }
    }

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        unbondingCancel.prepareForProgress(lifecycleOwner)
    }
}
