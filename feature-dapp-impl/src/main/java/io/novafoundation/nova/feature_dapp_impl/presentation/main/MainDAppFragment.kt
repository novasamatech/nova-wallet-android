package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.graphics.Rect
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.NestedAdapter
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentDappMainBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import javax.inject.Inject

class MainDAppFragment :
    BaseFragment<MainDAppViewModel, FragmentDappMainBinding>(),
    DappListAdapter.Handler,
    DAppHeaderAdapter.Handler,
    DappCategoriesAdapter.Handler {

    override fun createBinding() = FragmentDappMainBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { DAppHeaderAdapter(imageLoader, this) }

    private val dappsShimmering by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.layout_dapps_shimmering) }

    private val categoriesAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NestedAdapter(
            DappCategoriesAdapter(this),
            RecyclerView.HORIZONTAL,
            paddingInDp = Rect(16, 0, 16, 0)
        )
    }

    private val dappListAdapter by lazy(LazyThreadSafetyMode.NONE) { DappListAdapter(this) }

    override fun initViews() {
        binder.dappRecyclerView.applyStatusBarInsets()
        binder.dappRecyclerView.adapter = ConcatAdapter(headerAdapter, categoriesAdapter, dappsShimmering, dappListAdapter)
        binder.dappRecyclerView.addItemDecoration(DAppItemDecoration(requireContext()))
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
            categoriesAdapter.show(state is LoadingState.Loaded)
            if (state is LoadingState.Loaded) {
                categoriesAdapter.submitList(state.data.categories)
            }
        }
    }

    override fun onCategoryClicked(id: String) {
        viewModel.categorySelected(id)
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
