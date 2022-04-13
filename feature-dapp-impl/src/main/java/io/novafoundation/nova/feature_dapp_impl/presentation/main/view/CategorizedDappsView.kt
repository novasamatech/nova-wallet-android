package io.novafoundation.nova.feature_dapp_impl.presentation.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.DappCategoriesAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryState
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsCategories
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsCategoriesShimmer
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsDappsShimmer
import kotlinx.android.synthetic.main.view_categorized_dapps.view.categorizedDappsList
import javax.inject.Inject

typealias OnCategoryClickListener = (id: String) -> Unit

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

    private var dAppListEventHandler: DappListAdapter.Handler? = null
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

    fun setOnCategoryChangedListener(listener: OnCategoryClickListener) {
        onCategoryClickListener = listener
    }

    fun setOnDappListEventsHandler(handler: DappListAdapter.Handler) {
        dAppListEventHandler = handler
    }

    fun showCategories(state: DAppCategoryState) {
        categorizedDappsCategoriesShimmer.makeGone()
        categorizedDappsCategories.makeVisible()

        categoriesAdapter.submitList(state.categories) {
            state.selectedIndex?.let { scrollToCenter(it) }
        }
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
        dAppListEventHandler?.onItemClicked(item)
    }

    override fun onItemFavouriteClicked(item: DappModel) {
        dAppListEventHandler?.onItemFavouriteClicked(item)
    }

    override fun onItemClicked(id: String) {
        onCategoryClickListener?.invoke(id)
    }

    private fun scrollToCenter(position: Int) {
        categorizedDappsCategories.findViewHolderForAdapterPosition(position)?.let {
            val itemToScroll: Int = categorizedDappsCategories.getChildLayoutPosition(it.itemView)
            val centerOfScreen: Int = categorizedDappsCategories.width / 2 - it.itemView.width / 2

            categorizedDappsCategories.linearLayoutManager.scrollToPositionWithOffset(itemToScroll, centerOfScreen)
        }
    }

    private val RecyclerView.linearLayoutManager: LinearLayoutManager
        get() = layoutManager as LinearLayoutManager
}
