package io.novafoundation.nova.common.view.section

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.section_title_image_content.view.sectionContent
import kotlinx.android.synthetic.main.section_title_image_content.view.sectionImage
import kotlinx.android.synthetic.main.section_title_image_content.view.sectionTitle

class TitleImageContentSectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SectionView(R.layout.section_title_image_content,  context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        attrs?.let(::applyAttrs)
    }

    fun setTitle(title: String) {
        sectionTitle.text = title
    }

    fun setContent(content: String) {
        sectionContent.text = content
    }

    fun setImage(image: Drawable) {
        sectionImage.setImageDrawable(image)
    }

    fun loadImage(url: String) {
        sectionImage.load(url, imageLoader)
    }

    fun setContentEndIcon(@DrawableRes icon: Int, @ColorRes tint: Int? = null) {
        sectionContent.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 4, tint = tint)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TitleImageContentSectionView) { typedArray ->
        val title = typedArray.getString(R.styleable.TitleImageContentSectionView_sectionTitle)
        title?.let(::setTitle)

        val contentEndIcon = typedArray.getResourceIdOrNull(R.styleable.TitleImageContentSectionView_sectionContentEndIcon)

        contentEndIcon?.let {
            val contentEndTint = typedArray.getResourceIdOrNull(R.styleable.TitleImageContentSectionView_sectionContentEndIconTint)

            setContentEndIcon(contentEndIcon, contentEndTint)
        }
    }
}


