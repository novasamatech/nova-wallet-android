package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.UnbondingsAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.model.UnbondingModel
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingListContainer
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingsList
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingsMoreAction
import kotlinx.android.synthetic.main.view_unbondings.view.unbondingsPlaceholder

class UnbondingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val unbondingsAdapter = UnbondingsAdapter()

    init {
        View.inflate(context, R.layout.view_unbondings, this)

        background = context.getBlurDrawable()

        unbondingsList.adapter = unbondingsAdapter
    }

    fun setMoreActionClickListener(listener: OnClickListener) {
        unbondingsMoreAction.setOnClickListener(listener)
    }

    fun submitList(unbondings: List<UnbondingModel>) {
        unbondingsAdapter.submitList(unbondings)

        unbondingListContainer.setVisible(unbondings.isNotEmpty())
        unbondingsPlaceholder.setVisible(unbondings.isEmpty())
        unbondingsMoreAction.isEnabled = unbondings.isNotEmpty()
    }
}
