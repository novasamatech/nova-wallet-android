package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.section.SectionView
import io.novafoundation.nova.feature_wallet_api.R

class PriceSectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SectionView(R.layout.section_price, context, attrs, defStyleAttr) {

    init {
        attrs?.let(::applyAttrs)
    }

    fun setPrice(token: String, fiat: String?) {
        sectionPriceToken.text = token
        sectionPriceFiat.setTextOrHide(fiat)
    }

    fun setTitle(title: String) {
        sectionTitle.text = title
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PriceSectionView) {
        val title = it.getString(R.styleable.PriceSectionView_sectionTitle)
        title?.let(::setTitle)
    }
}
