package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappListAdapter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainCategorizedDapps
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainContainer
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainIcon
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainSearch

class MainDAppFragment : BaseFragment<MainDAppViewModel>(), DappListAdapter.Handler {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_main, container, false)
    }

    override fun initViews() {
        dappMainContainer.applyStatusBarInsets()

        dappMainIcon.setOnClickListener { viewModel.accountIconClicked() }

        dappMainCategorizedDapps.setOnCategoryChangedListener {
            viewModel.categorySelected(it)
        }
        dappMainCategorizedDapps.setOnDappListEventsHandler(this)

        dappMainSearch.setOnClickListener {
            viewModel.searchClicked()
        }
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

        viewModel.currentAddressIconFlow.observe(dappMainIcon::setImageDrawable)

        viewModel.shownDAppsStateFlow.observe { state ->
            when (state) {
                is LoadingState.Loaded -> dappMainCategorizedDapps.showDapps(state.data)
                is LoadingState.Loading -> dappMainCategorizedDapps.showDappsShimmering()
            }
        }

        viewModel.categoriesStateFlow.observe { state ->
            when (state) {
                is LoadingState.Loaded -> dappMainCategorizedDapps.showCategories(state.data)
                is LoadingState.Loading -> dappMainCategorizedDapps.showCategoriesShimmering()
            }
        }
    }

    override fun onItemClicked(item: DappModel) {
        viewModel.dappClicked(item)
    }

    override fun onItemFavouriteClicked(item: DappModel) {
        viewModel.dappFavouriteClicked(item)
    }
}
