package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingCancel
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingRedeem
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingsList

class UnbondingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val unbondingsAdapter = UnbondingsAdapter()

    init {
        View.inflate(context, R.layout.view_unbondings, this)

        background = context.getRoundedCornerDrawable(R.color.black_48)

        unbondingsList.adapter = unbondingsAdapter
    }

    fun onRedeemClicked(action: () -> Unit) {
        unbondingRedeem.setOnClickListener { action() }
    }

    fun onCancelClicked(action: () -> Unit) {
        unbondingCancel.setOnClickListener { action() }
    }

    fun setState(state: UnbondingMixin.State) {
        when (state) {
            UnbondingMixin.State.Empty -> makeGone()
            is UnbondingMixin.State.HaveUnbondings -> {
                makeVisible()

                unbondingsAdapter.submitList(state.unbondings)

                unbondingRedeem.isEnabled = state.redeemEnabled
                unbondingCancel.isEnabled = state.cancelEnabled
            }
        }
    }
}
