package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.section.SectionView
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.SectionPriceBinding

class PriceSectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SectionView(context, attrs, defStyleAttr) {

    private val binder = SectionPriceBinding.inflate(inflater(), this)

    init {
        attrs?.let(::applyAttrs)
    }

    fun setPrice(token: CharSequence, fiat: CharSequence?) {
        binder.sectionPriceToken.text = token
        binder.sectionPriceFiat.setTextOrHide(fiat)
    }

    fun setTitle(title: String) {
        binder.sectionTitle.text = title
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PriceSectionView) {
        val title = it.getString(R.styleable.PriceSectionView_sectionTitle)
        title?.let(::setTitle)
    }
}
