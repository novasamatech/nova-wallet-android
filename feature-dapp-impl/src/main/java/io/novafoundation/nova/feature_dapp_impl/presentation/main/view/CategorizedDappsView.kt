package io.novafoundation.nova.feature_dapp_impl.presentation.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.DappCategoriesAdapter
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsCategories
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsCategoriesShimmer
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsDappsShimmer
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsList
import javax.inject.Inject

typealias OnDappClickListener = (DappModel) -> Unit
typealias OnCategoryClickListener = (position: Int) -> Unit

class CategorizedDappsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle),
    DappListAdapter.Handler,
    DappCategoriesAdapter.Handler {

    @Inject lateinit var imageLoader: ImageLoader

    private val dappListAdapter by lazy(LazyThreadSafetyMode.NONE) { DappListAdapter(this, imageLoader) }
    private val categoriesAdapter by lazy(LazyThreadSafetyMode.NONE) { DappCategoriesAdapter(this) }

    private var onDappClickListener: OnDappClickListener? = null
    private var onCategoryClickListener: OnCategoryClickListener? = null

    init {
        View.inflate(context, R.layout.view_categorized_dapps, this)
        orientation = VERTICAL

        background = context.getRoundedCornerDrawable(fillColorRes = R.color.black_48)

        FeatureUtils.getFeature<DAppFeatureComponent>(
            context,
            DAppFeatureApi::class.java
        ).inject(this)

        categorizedDappsList.adapter = dappListAdapter
        categorizedDappsCategories.adapter = categoriesAdapter
        clipToOutline = true // for round corners
    }

    fun setSelectedCategory(position: Int) {
        categoriesAdapter.setSelectedCategory(position)
    }

    fun setOnCategoryChangedListener(listener: (position: Int) -> Unit) {
        onCategoryClickListener = listener
    }

    fun setOnDappClickedListener(listener: OnDappClickListener) {
        onDappClickListener = listener
    }

    fun showCategories(categories: List<DappCategory>) {
        categorizedDappsCategoriesShimmer.makeGone()
        categorizedDappsCategories.makeVisible()

        categoriesAdapter.submitList(categories)
    }

    fun showDapps(dapps: List<DappModel>) {
        categorizedDappsDappsShimmer.makeGone()
        categorizedDappsList.makeVisible()

        dappListAdapter.submitList(dapps)
    }

    fun showDappsShimmering() {
        categorizedDappsDappsShimmer.makeVisible()
        categorizedDappsList.makeGone()
    }

    fun showCategoriesShimmering() {
        categorizedDappsCategoriesShimmer.makeVisible()
        categorizedDappsCategories.makeGone()
    }

    override fun onItemClicked(item: DappModel) {
        onDappClickListener?.invoke(item)
    }

    override fun onItemClicked(position: Int) {
        onCategoryClickListener?.invoke(position)
    }
}
