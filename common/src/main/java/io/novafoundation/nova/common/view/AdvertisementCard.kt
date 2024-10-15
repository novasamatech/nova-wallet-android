package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewAdvertisementCardBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

class AdvertisementCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewAdvertisementCardBinding.inflate(inflater(), this)

    val action: PrimaryButton
        get() = binder.advertisementCardButton

    init {
        cardElevation = 0f
        radius = 12f.dpF(context)
        strokeWidth = 1.dp(context)
        strokeColor = context.getColor(R.color.container_border)

        attrs?.let(::applyAttrs)

        updatePadding(bottom = 16.dp)
    }

    fun setupAction(lifecycleOwner: LifecycleOwner, onClicked: (View) -> Unit) {
        action.prepareForProgress(lifecycleOwner)
        action.setOnClickListener(onClicked)
    }

    fun setOnLearnMoreClickedListener(onClicked: (View) -> Unit) {
        binder.advertisementCardLearnMoreArrow.setOnClickListener(onClicked)
        binder.advertisementCardLearnMoreContent.setOnClickListener(onClicked)
    }

    fun setOnCloseClickListener(listener: OnClickListener?) {
        binder.advertisementCardClose.setOnClickListener(listener)
    }

    fun setModel(model: AdvertisementCardModel) {
        binder.advertisementCardTitle.text = model.title
        binder.advertisementCardSubTitle.text = model.subtitle
        binder.advertisementCardImage.setImageResource(model.imageRes)
        binder.advertisementCardBackground.setBackgroundResource(model.bannerBackgroundRes)
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.AdvertisementCard) {
        val actionLabel = it.getString(R.styleable.AdvertisementCard_action)
        action.setTextOrHide(actionLabel)

        val learnMore = it.getString(R.styleable.AdvertisementCard_learnMore)
        binder.advertisementCardLearnMoreGroup.setVisible(learnMore != null)
        binder.advertisementCardLearnMoreContent.text = learnMore

        val title = it.getString(R.styleable.AdvertisementCard_title)
        binder.advertisementCardTitle.text = title

        val subtitle = it.getString(R.styleable.AdvertisementCard_subtitle)
        binder.advertisementCardSubTitle.text = subtitle

        val image = it.getDrawable(R.styleable.AdvertisementCard_image)
        binder.advertisementCardImage.setImageDrawable(image)

        val bannerBackground = it.getDrawable(R.styleable.AdvertisementCard_advertisementCardBackground)
        binder.advertisementCardBackground.background = bannerBackground

        val showClose = it.getBoolean(R.styleable.AdvertisementCard_showClose, false)
        binder.advertisementCardClose.isVisible = showClose
    }
}

class AdvertisementCardModel(
    val title: String,
    val subtitle: String,
    @DrawableRes val imageRes: Int,
    @DrawableRes val bannerBackgroundRes: Int,
)
