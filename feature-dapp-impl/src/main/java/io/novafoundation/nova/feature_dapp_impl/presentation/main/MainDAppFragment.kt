package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Rect
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentDappMainBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappCategoryListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import javax.inject.Inject

class MainDAppFragment :
    BaseFragment<MainDAppViewModel, FragmentDappMainBinding>(),
    DAppClickHandler,
    DAppHeaderAdapter.Handler,
    DappCategoriesAdapter.Handler {

    override fun createBinding() = FragmentDappMainBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { DAppHeaderAdapter(imageLoader, this, this, this) }

    private val dappsShimmering by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering) }

    private val dappCategoriesListAdapter by lazy(LazyThreadSafetyMode.NONE) { DappCategoryListAdapter(this) }

    override fun initViews() {
        binder.dappRecyclerViewCatalog.applyStatusBarInsets()
        binder.dappRecyclerViewCatalog.adapter = ConcatAdapter(headerAdapter, dappsShimmering, dappCategoriesListAdapter)
        binder.dappRecyclerViewCatalog.itemAnimator = null
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .mainComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MainDAppViewModel) {
        observeBrowserEvents(viewModel)

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
            headerAdapter.setFavorites(it)
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
}
