package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceLockAmount
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceLockedIcon
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceLockedTitle
import kotlinx.android.synthetic.main.view_governance_locks.view.governanceUnlockBadge

class GovernanceLocksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_governance_locks, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)

        governanceLockAmount.isEnabled = false

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.GovernanceLocksView) {
        val title = it.getString(R.styleable.GovernanceLocksView_governanceLocksView_label)
        title?.let(::setTitle)

        val leftIcon = it.getDrawable(R.styleable.GovernanceLocksView_governanceLocksView_icon)
        leftIcon?.let(::setLeftIcon)
    }

    fun setTitle(title: String) {
        governanceLockedTitle.text = title
    }

    fun setLeftIcon(icon: Drawable?) {
        governanceLockedIcon.setImageDrawable(icon)
    }

    fun setModel(model: GovernanceLocksModel) {
        governanceLockedTitle.text = model.title
        governanceLockAmount.setTextOrHide(model.amount)
        governanceUnlockBadge.isVisible = model.hasUnlockableLocks
    }
}

class GovernanceLocksModel(
    val amount: CharSequence?,
    val title: String,
    val hasUnlockableLocks: Boolean
)
