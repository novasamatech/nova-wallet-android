package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewUnbondingsBinding

class UnbondingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val unbondingsAdapter = UnbondingsAdapter()

    private val binder = ViewUnbondingsBinding.inflate(inflater(), this)

    init {
        background = context.getRoundedCornerDrawable(R.color.block_background)

        binder.unbondingsList.adapter = unbondingsAdapter
    }

    fun onRedeemClicked(action: () -> Unit) {
        binder.unbondingRedeem.setOnClickListener { action() }
    }

    fun onCancelClicked(action: () -> Unit) {
        binder.unbondingCancel.setOnClickListener { action() }
    }

    fun setState(state: UnbondingState) {
        when (state) {
            UnbondingState.Empty -> makeGone()
            is UnbondingState.HaveUnbondings -> {
                makeVisible()

                unbondingsAdapter.submitList(state.unbondings)

                binder.unbondingRedeem.isEnabled = state.redeemEnabled
                binder.unbondingCancel.setState(state.cancelState)
            }
        }
    }

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        binder.unbondingCancel.prepareForProgress(lifecycleOwner)
    }
}
