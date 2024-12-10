package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewAmountChangesBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.AmountChangeModel

class AmountChangesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes),
    WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewAmountChangesBinding.inflate(inflater(), this)

    init {
        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let { applyAttributes(it) }
    }

    fun setFrom(value: CharSequence?) {
        if (value != null) {
            binder.valueChangesFrom.text = value
            binder.valueChangesFromGroup.makeVisible()
        } else {
            binder.valueChangesFromGroup.makeGone()
        }
    }

    fun setTo(value: CharSequence) {
        binder.valueChangesTo.text = value
    }

    fun setDifference(@DrawableRes icon: Int, text: CharSequence, @ColorRes textColor: Int) {
        binder.valueChangesDifference.makeVisible()

        binder.valueChangesDifference.setDrawableStart(icon, widthInDp = 16, tint = textColor)
        binder.valueChangesDifference.text = text
        binder.valueChangesDifference.setTextColorRes(textColor)
    }

    fun hideDifference() {
        binder.valueChangesDifference.makeGone()
    }

    fun setTitle(title: String) {
        binder.valueChangesTitle.text = title
    }

    fun setIcon(icon: Drawable?) = binder.valueChangesIcon.letOrHide(icon, binder.valueChangesIcon::setImageDrawable)

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
