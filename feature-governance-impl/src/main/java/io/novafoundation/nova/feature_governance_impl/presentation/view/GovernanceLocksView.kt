package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceLockAmount
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceUnlockBadge

class GovernanceLocksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_governance_locks, this)

        with(context) {
            background = addRipple(getBlockDrawable(), mask = getRippleMask())
        }

        governanceLockAmount.isEnabled = false
    }

    fun setModel(model: GovernanceLocksModel) {
        governanceLockAmount.setText(model.amount)
        governanceUnlockBadge.isVisible = model.hasUnlockableLocks
    }
}

class GovernanceLocksModel(
    val amount: String,
    val hasUnlockableLocks: Boolean
)
