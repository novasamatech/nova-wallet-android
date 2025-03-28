package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.recyclerView.space.SpaceBetween
import io.novafoundation.nova.common.utils.recyclerView.space.addSpaceItemDecoration
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannerAdapter
import io.novafoundation.nova.feature_banners_api.presentation.bindWithAdapter
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappCategoryListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappCategoryViewHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_dapp_main.dappRecyclerViewCatalog

class MainDAppFragment :
    BaseFragment<MainDAppViewModel>(),
    DAppClickHandler,
    DAppHeaderAdapter.Handler,
    DappCategoriesAdapter.Handler,
    MainFavoriteDAppsAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { DAppHeaderAdapter(imageLoader, this, this) }

    private val bannerAdapter: PromotionBannerAdapter by lazy(LazyThreadSafetyMode.NONE) { PromotionBannerAdapter(closable = false) }

    private val favoritesAdapter: MainFavoriteDAppsAdapter by lazy(LazyThreadSafetyMode.NONE) { MainFavoriteDAppsAdapter(this, this, imageLoader) }

    private val dappsShimmering by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering) }

    private val dappCategoriesListAdapter by lazy(LazyThreadSafetyMode.NONE) { DappCategoryListAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_main, container, false)
    }

    override fun initViews() {
        dappRecyclerViewCatalog.applyStatusBarInsets()
        dappRecyclerViewCatalog.adapter = ConcatAdapter(headerAdapter, bannerAdapter, favoritesAdapter, dappsShimmering, dappCategoriesListAdapter)
        dappRecyclerViewCatalog.itemAnimator = null
        setupRecyclerViewSpacing()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .mainComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MainDAppViewModel) {
        observeBrowserEvents(viewModel)
        viewModel.bannersMixin.bindWithAdapter(bannerAdapter) {
            dappRecyclerViewCatalog?.invalidateItemDecorations()
        }

        viewModel.selectedWalletFlow.observe(headerAdapter::setWallet)

        viewModel.shownDAppsStateFlow.observe { state ->
            when (state) {
                is LoadingState.Loaded -> {
                    dappsShimmering.show(false)
                    dappCategoriesListAdapter.submitList(state.data)
                }

                is LoadingState.Loading -> {
                    dappsShimmering.show(true)
                    dappCategoriesListAdapter.submitList(listOf())
                }

                else -> {}
            }
        }

        viewModel.categoriesStateFlow.observe { state ->
            headerAdapter.showCategoriesShimmering(state is LoadingState.Loading)
            if (state is LoadingState.Loaded) {
                headerAdapter.setCategories(state.data.categories)
            }
        }

        viewModel.favoriteDAppsUIFlow.observe {
            favoritesAdapter.show(it.isNotEmpty())
            favoritesAdapter.setDApps(it)
        }
    }

    override fun onCategoryClicked(id: String) {
        viewModel.openCategory(id)
    }

    override fun onDAppClicked(item: DappModel) {
        viewModel.dappClicked(item)
    }

    override fun onWalletClick() {
        viewModel.accountIconClicked()
    }

    override fun onSearchClick() {
        viewModel.searchClicked()
    }

    override fun onManageClick() {
        viewModel.manageClicked()
    }

    override fun onManageFavoritesClick() {
        viewModel.openFavorites()
    }

    private fun setupRecyclerViewSpacing() {
        dappRecyclerViewCatalog.addSpaceItemDecoration {
            // Add extra space between items
            add(SpaceBetween(DappCategoryViewHolder, spaceDp = 8))
        }
    }
}
