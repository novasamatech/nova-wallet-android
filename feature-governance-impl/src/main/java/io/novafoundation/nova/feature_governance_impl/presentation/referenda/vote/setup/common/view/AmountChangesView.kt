package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.AmountChangeModel

class AmountChangesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes),
    WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_amount_changes, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let { applyAttributes(it) }
    }

    fun setFrom(value: String?) {
        if (value != null) {
            valueChangesFrom.text = value
            valueChangesFromGroup.makeVisible()
        } else {
            valueChangesFromGroup.makeGone()
        }
    }

    fun setTo(value: String) {
        valueChangesTo.text = value
    }

    fun setDifference(@DrawableRes icon: Int, text: String, @ColorRes textColor: Int) {
        valueChangesDifference.makeVisible()

        valueChangesDifference.setDrawableStart(icon, widthInDp = 16, tint = textColor)
        valueChangesDifference.text = text
        valueChangesDifference.setTextColorRes(textColor)
    }

    fun hideDifference() {
        valueChangesDifference.makeGone()
    }

    fun setTitle(title: String) {
        valueChangesTitle.text = title
    }

    fun setIcon(icon: Drawable?) = valueChangesIcon.letOrHide(icon, valueChangesIcon::setImageDrawable)

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.AmountChangesView) { typedArray ->
        val title = typedArray.getString(R.styleable.AmountChangesView_amountChanges_title)
        title?.let(::setTitle)

        val icon = typedArray.getDrawable(R.styleable.AmountChangesView_amountChanges_icon)
        setIcon(icon)
    }
}

fun AmountChangesView.setAmountChangeModel(model: AmountChangeModel) {
    setFrom(model.from)
    setTo(model.to)

    val difference = model.difference

    if (difference != null) {
        setDifference(difference.icon, difference.text, difference.color)
    } else {
        hideDifference()
    }
}
