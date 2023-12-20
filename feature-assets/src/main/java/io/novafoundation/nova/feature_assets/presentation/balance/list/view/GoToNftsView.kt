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
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftCounter
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreview1
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreview2
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreview3
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreviewHolder1
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreviewHolder2
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftPreviewHolder3
import kotlinx.android.synthetic.main.view_go_to_nfts.view.goToNftsShimmer
import javax.inject.Inject

class GoToNftsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions {

    @Inject
    lateinit var imageLoader: ImageLoader

    override val providedContext: Context = context

    private val previewViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(goToNftPreview1, goToNftPreview2, goToNftPreview3)
    }

    private val previewHolders by lazy(LazyThreadSafetyMode.NONE) {
        listOf(goToNftPreviewHolder1, goToNftPreviewHolder2, goToNftPreviewHolder3)
    }

    init {
        View.inflate(context, R.layout.view_go_to_nfts, this)

        background = addRipple(getRoundedCornerDrawable(R.color.block_background))

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            context,
            AssetsFeatureApi::class.java
        ).inject(this)
    }

    fun setNftCount(countLabel: String?) {
        goToNftCounter.text = countLabel
    }

    fun setPreviews(previews: List<NftPreviewUi>?) {
        setVisible(previews != null && previews.isNotEmpty())
        val shouldShowLoading = previews == null || previews.all { it is LoadingState.Loading }

        if (shouldShowLoading) {
            previewHolders.forEach(View::makeGone)
            goToNftsShimmer.makeVisible()
        } else {
            goToNftsShimmer.makeGone()

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
}
