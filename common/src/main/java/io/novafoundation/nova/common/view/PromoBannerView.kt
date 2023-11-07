package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_promo_banner.view.promoBannerClose
import kotlinx.android.synthetic.main.view_promo_banner.view.promoBannerDescription
import kotlinx.android.synthetic.main.view_promo_banner.view.promoBannerImage
import kotlinx.android.synthetic.main.view_promo_banner.view.promoBannerTitle

class PromoBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_promo_banner, this)

        attrs?.let(::applyAttributes)
    }

    fun setOnCloseClickListener(listener: OnClickListener?) {
        promoBannerClose.setOnClickListener(listener)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PromoBannerView) { typedArray ->
        val image = typedArray.getDrawable(R.styleable.PromoBannerView_promoBanner_image)
        promoBannerImage.setImageDrawable(image)

        val background = typedArray.getDrawable(R.styleable.PromoBannerView_promoBanner_background)
        setBackground(background)

        val title = typedArray.getString(R.styleable.PromoBannerView_promoBanner_title)
        promoBannerTitle.text = title

        val description = typedArray.getString(R.styleable.PromoBannerView_promoBanner_description)
        promoBannerDescription.text = description
    }
}
