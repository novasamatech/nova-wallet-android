package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewPromoBannerBinding
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes

class PromoBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewPromoBannerBinding.inflate(inflater(), this)

    init {
        attrs?.let(::applyAttributes)
    }

    fun setOnCloseClickListener(listener: OnClickListener?) {
        binder.promoBannerClose.setOnClickListener(listener)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PromoBannerView) { typedArray ->
        val image = typedArray.getDrawable(R.styleable.PromoBannerView_promoBanner_image)
        binder.promoBannerImage.setImageDrawable(image)

        val background = typedArray.getDrawable(R.styleable.PromoBannerView_promoBanner_background)
        setBackground(background)

        val title = typedArray.getString(R.styleable.PromoBannerView_promoBanner_title)
        binder.promoBannerTitle.text = title

        val description = typedArray.getString(R.styleable.PromoBannerView_promoBanner_description)
        binder.promoBannerDescription.text = description
    }
}
