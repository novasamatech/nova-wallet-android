package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewGovernanceLocksBinding

class GovernanceLocksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewGovernanceLocksBinding.inflate(inflater(), this)

    init {
        View.inflate(context, R.layout.view_governance_locks, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)

        binder.governanceLockAmount.isEnabled = false

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.GovernanceLocksView) {
        val title = it.getString(R.styleable.GovernanceLocksView_governanceLocksView_label)
        title?.let(::setTitle)

        val leftIcon = it.getDrawable(R.styleable.GovernanceLocksView_governanceLocksView_icon)
        leftIcon?.let(::setLeftIcon)
    }

    fun setTitle(title: String) {
        binder.governanceLockedTitle.text = title
    }

    fun setLeftIcon(icon: Drawable?) {
        binder.governanceLockedIcon.setImageDrawable(icon)
    }

    fun setModel(model: GovernanceLocksModel) {
        binder.governanceLockedTitle.text = model.title
        binder.governanceLockAmount.setTextOrHide(model.amount)
        binder.governanceUnlockBadge.isVisible = model.hasUnlockableLocks
    }
}

class GovernanceLocksModel(
    val amount: String?,
    val title: String,
    val hasUnlockableLocks: Boolean
)
