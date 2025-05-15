package io.novafoundation.nova.feature_pay_impl.presentation.shop.main

import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.list.StubItemsAdapter
import io.novafoundation.nova.common.utils.onScrollPositionChanged
import io.novafoundation.nova.feature_pay_api.di.PayFeatureApi
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.databinding.FragmentShopBinding
import io.novafoundation.nova.feature_pay_impl.di.PayFeatureComponent
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.adapter.ShopBrandsAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.ShopHeaderAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.adapter.ShopPaginationLoadingAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.ShopPopularBrandsAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.ShopPurchasesAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.ShopSearchAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.ShopUnavailableAccountPlaceholderAdapter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem
import javax.inject.Inject

class ShopFragment : BaseFragment<ShopViewModel, FragmentShopBinding>(),
    ShopPopularBrandsAdapter.Handler,
    ShopSearchAdapter.Handler,
    ShopBrandsAdapter.Handler, ShopPurchasesAdapter.Handler {

    override fun createBinding() = FragmentShopBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopHeaderAdapter() }

    private val shopPurchasesAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopPurchasesAdapter(this) }

    private val popularBrandsAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopPopularBrandsAdapter(this) }

    private val searchAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopSearchAdapter(this) }

    private val brandsAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopBrandsAdapter(imageLoader, this) }

    private val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { StubItemsAdapter(10, R.layout.item_brand_shimmering) }

    private val shopPaginationLoadingAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopPaginationLoadingAdapter() }

    private val unavailableWalletAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopUnavailableAccountPlaceholderAdapter() }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(
            headerAdapter,
            shopPurchasesAdapter,
            popularBrandsAdapter,
            searchAdapter,
            brandsAdapter,
            shimmeringAdapter,
            shopPaginationLoadingAdapter,
            unavailableWalletAdapter
        )
    }

    override fun initViews() {
        binder.shopList.adapter = adapter
        binder.shopList.itemAnimator = null

        binder.shopList.setHasFixedSize(true)
        binder.shopList.onScrollPositionChanged(viewModel::onScrolled)

        // TODO: awaits for design approve
        popularBrandsAdapter.show(false)
    }

    override fun inject() {
        FeatureUtils.getFeature<PayFeatureComponent>(requireContext(), PayFeatureApi::class.java)
            .shopComponentFactory()
            .create(fragment = this)
            .inject(this)
    }

    override fun subscribe(viewModel: ShopViewModel) {
        viewModel.brandsListState.observe {
            val showUnavailableWalletCase = it is BrandsListState.UnavailableWallet
            val showBrands = it is BrandsListState.Brands
            val showLoading = it is BrandsListState.Brands && it.brands.isLoading

            unavailableWalletAdapter.show(showUnavailableWalletCase)

            headerAdapter.show(showBrands)
            searchAdapter.show(showBrands)
            shopPaginationLoadingAdapter.show(showBrands && !showLoading)
            headerAdapter.showShimmering(showLoading)
            shimmeringAdapter.show(showLoading)
            brandsAdapter.submitList(it.getBrandsOrNull())
        }

        viewModel.maxCashback.observe(headerAdapter::setHeaderText)

        viewModel.paginationMixin.isNewPageLoading.observe { shopPaginationLoadingAdapter.setInvisible(!it) }

        viewModel.purchasedCardsState.observe {
            when (it) {
                PurchasedCardsState.Empty -> shopPurchasesAdapter.show(false)
                is PurchasedCardsState.Content -> {
                    shopPurchasesAdapter.show(true)
                    shopPurchasesAdapter.setPurchasesQuantity(it.quantity)
                }
            }
        }
    }

    override fun onPopularBrandClick(brandModel: ShopBrandRVItem) {
        viewModel.brandClicked(brandModel)
    }

    override fun onSearchClick() {
        viewModel.onSearchClick()
    }

    override fun onBrandClick(brandModel: ShopBrandRVItem) {
        viewModel.brandClicked(brandModel)
    }

    override fun onPurchasesClick() {
        viewModel.purchasesClicked()
    }
}
