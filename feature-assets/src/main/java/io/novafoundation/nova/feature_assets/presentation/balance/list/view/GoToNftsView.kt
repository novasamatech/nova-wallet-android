package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.presentation.isLoading
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ViewGoToNftsBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi

import javax.inject.Inject

class GoToNftsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions {

    private val binder = ViewGoToNftsBinding.inflate(inflater(), this)

    @Inject
    lateinit var imageLoader: ImageLoader

    override val providedContext: Context = context

    private val previewViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(binder.goToNftPreview1, binder.goToNftPreview2, binder.goToNftPreview3)
    }

    private val previewHolders by lazy(LazyThreadSafetyMode.NONE) {
        listOf(binder.goToNftPreviewHolder1, binder.goToNftPreviewHolder2, binder.goToNftPreviewHolder3)
    }

    init {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            context,
            AssetsFeatureApi::class.java
        ).inject(this)
    }

    fun setNftCount(countLabel: MaskableModel<String>?) {
        if (countLabel == null) return
        binder.goToNftCounter.setMaskableText(countLabel)
    }

    fun setPreviews(previewsMaskable: MaskableModel<List<NftPreviewUi>>?) {
        when (previewsMaskable) {
            is MaskableModel.Hidden -> maskPreviews()
            is MaskableModel.Unmasked -> setPreviews(previewsMaskable.value)
            null -> makeGone()
        }
    }

    fun setPreviews(previews: List<NftPreviewUi>?) {
        setVisible(!previews.isNullOrEmpty())
        val shouldShowLoading = previews == null || previews.all { it is LoadingState.Loading }

        if (shouldShowLoading) {
            previewHolders.forEach(View::makeGone)
            binder.goToNftsShimmer.makeVisible()
        } else {
            binder.goToNftsShimmer.makeGone()

            previewHolders.forEachIndexed { index, view ->
                val previewContent = previews!!.getOrNull(index)

                if (previewContent == null || previewContent.isLoading) {
                    view.makeGone()
                } else {
                    view.makeVisible()
                    previewViews[index].load(previewContent.dataOrNull, imageLoader)
                }
            }
        }
    }

    private fun maskPreviews() {
        makeVisible()
        val images = listOf(R.drawable.ic_blue_siri, R.drawable.ic_yellow_siri, R.drawable.ic_pink_siri)
        images.forEachIndexed { index, imageRes ->
            previewHolders[index].makeVisible()
            previewViews[index].setImageResource(imageRes)
        }
    }
}
