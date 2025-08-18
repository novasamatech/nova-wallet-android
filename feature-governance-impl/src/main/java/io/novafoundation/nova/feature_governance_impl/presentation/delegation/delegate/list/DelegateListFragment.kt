package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.input.chooser.setupListChooserMixinBottomSheet
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentDelegateListBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter.DelegateListAdapter

import javax.inject.Inject

class DelegateListFragment :
    BaseFragment<DelegateListViewModel, FragmentDelegateListBinding>(),
    DelegateListAdapter.Handler,
    DelegateBannerAdapter.Handler,
    DelegateSortAndFilterAdapter.Handler {

    override fun createBinding() = FragmentDelegateListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val bannerAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateBannerAdapter(this) }
    private val sortAndFilterAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateSortAndFilterAdapter(this) }
    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.item_delegates_shimmering) }
    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(bannerAdapter, sortAndFilterAdapter, placeholderAdapter, delegateListAdapter) }

    override fun initViews() {
        binder.delegateListList.itemAnimator = null
        binder.delegateListList.setHasFixedSize(true)
        binder.delegateListList.adapter = adapter

        binder.delegateListToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.delegateListToolbar.setRightActionClickListener { viewModel.openSearch() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .delegateListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DelegateListViewModel) {
        setupListChooserMixinBottomSheet(viewModel.sortingMixin)
        setupListChooserMixinBottomSheet(viewModel.filteringMixin)

        viewModel.sortingMixin.selectedOption.observe {
            sortAndFilterAdapter.setSortingValue(it.display)
        }

        viewModel.filteringMixin.selectedOption.observe {
            sortAndFilterAdapter.setFilteringMixin(it.display)
        }

        viewModel.shouldShowBannerFlow.observe {
            bannerAdapter.showBanner(it)
        }

        viewModel.delegateModels.observeWhenVisible {
            when (it) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loaded -> {
                    placeholderAdapter.show(false)
                    delegateListAdapter.submitListPreservingViewPoint(it.data, binder.delegateListList)
                }
                ExtendedLoadingState.Loading -> {
                    placeholderAdapter.show(true)
                    delegateListAdapter.submitList(emptyList())
                }
            }
        }
    }

    override fun closeBanner() {
        viewModel.closeBanner()
    }

    override fun describeYourselfClicked() {
        viewModel.openBecomingDelegateTutorial()
    }

    override fun itemClicked(position: Int) {
        viewModel.delegateClicked(position)
    }

    override fun filteringClicked() {
        viewModel.filteringMixin.selectorClicked()
    }

    override fun sortingClicked() {
        viewModel.sortingMixin.selectorClicked()
    }
}
