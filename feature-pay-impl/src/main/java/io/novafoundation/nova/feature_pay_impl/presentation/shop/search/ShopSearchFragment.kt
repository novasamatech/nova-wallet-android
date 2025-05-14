package io.novafoundation.nova.feature_pay_impl.presentation.shop.search

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.list.StubItemsAdapter
import io.novafoundation.nova.common.utils.applyImeInsetts
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.onScrollPositionChanged
import io.novafoundation.nova.feature_pay_api.di.PayFeatureApi
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.databinding.FragmentShopSearchBinding
import io.novafoundation.nova.feature_pay_impl.di.PayFeatureComponent
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.adapter.ShopBrandsAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.adapter.ShopPaginationLoadingAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem
import javax.inject.Inject

class ShopSearchFragment :
    BaseFragment<ShopSearchViewModel, FragmentShopSearchBinding>(),
    ShopBrandsAdapter.Handler {

    override fun createBinding() = FragmentShopSearchBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val brandsAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopBrandsAdapter(imageLoader, this) }

    private val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { StubItemsAdapter(10, R.layout.item_brand_shimmering) }

    private val shopPaginationLoadingAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopPaginationLoadingAdapter() }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(
            brandsAdapter,
            shimmeringAdapter,
            shopPaginationLoadingAdapter
        )
    }

    override fun initViews() {
        binder.shopSearchToolbar.applyStatusBarInsets()
        binder.shopSearchContainer.applyImeInsetts()

        binder.shopSearchList.adapter = adapter
        binder.shopSearchList.itemAnimator = null

        binder.shopSearchList.setHasFixedSize(true)
        binder.shopSearchList.onScrollPositionChanged(viewModel::onScrolled)

        binder.shopSearchToolbar.cancel.setOnClickListener { viewModel.backClicked() }

        binder.shopSearchToolbar.searchInput.requestFocus()
        binder.shopSearchToolbar.searchInput.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<PayFeatureComponent>(requireContext(), PayFeatureApi::class.java)
            .shopSearchComponentFactory()
            .create(fragment = this)
            .inject(this)
    }

    override fun subscribe(viewModel: ShopSearchViewModel) {
        binder.shopSearchToolbar.searchInput.content.bindTo(viewModel.paginationMixin.searchInput, lifecycleScope)

        viewModel.brandsListState.observe {
            shopPaginationLoadingAdapter.show(!it.isLoading())
            shimmeringAdapter.show(it.isLoading())
            brandsAdapter.submitList(it.dataOrNull)
        }

        viewModel.paginationMixin.isNewPageLoading.observe { shopPaginationLoadingAdapter.setInvisible(!it) }
    }

    override fun onBrandClick(brandModel: ShopBrandRVItem) {
        viewModel.brandClicked(brandModel)
    }
}
