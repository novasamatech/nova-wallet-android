package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_dapp_main.dappRecyclerView
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappCategories

class MainDAppFragment :
    BaseFragment<MainDAppViewModel>(),
    DappListAdapter.Handler,
    DAppHeaderAdapter.Handler,
    DappCategoriesAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { DAppHeaderAdapter(imageLoader, this, this) }

    private val dappsShimmering by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering) }

    private val dappListAdapter by lazy(LazyThreadSafetyMode.NONE) { DappListAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_main, container, false)
    }

    override fun initViews() {
        dappRecyclerView.applyStatusBarInsets()
        dappRecyclerView.adapter = ConcatAdapter(headerAdapter, dappsShimmering, dappListAdapter)
        dappRecyclerView.addItemDecoration(DAppItemDecoration(requireContext()))
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .mainComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MainDAppViewModel) {
        observeBrowserEvents(viewModel)
        setupRemoveFavouritesConfirmation(viewModel.removeFavouriteConfirmationAwaitable)

        viewModel.selectedWalletFlow.observe(headerAdapter::setWallet)

        viewModel.shownDAppsStateFlow.observe { state ->
            when (state) {
                is LoadingState.Loaded -> {
                    dappsShimmering.show(false)
                    dappListAdapter.submitList(state.data)
                }

                is LoadingState.Loading -> {
                    dappsShimmering.show(true)
                    dappListAdapter.submitList(listOf())
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
    }

    override fun onCategoryClicked(id: String) {
        viewModel.openCategory(id)
    }

    override fun onDAppClicked(item: DappModel) {
        viewModel.dappClicked(item)
    }

    override fun onItemFavouriteClicked(item: DappModel) {
        viewModel.dappFavouriteClicked(item)
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
}
