package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentYourDelegationsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter.DelegateListAdapter
import javax.inject.Inject

class YourDelegationsFragment :
    BaseFragment<YourDelegationsViewModel, FragmentYourDelegationsBinding>(),
    DelegateListAdapter.Handler {

    override fun createBinding() = FragmentYourDelegationsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.item_delegates_shimmering) }
    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(placeholderAdapter, delegateListAdapter) }

    override fun initViews() {
        binder.yourDelegationsList.itemAnimator = null
        binder.yourDelegationsList.adapter = adapter

        binder.yourDelegationsToolbar.applyStatusBarInsets()
        binder.yourDelegationsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.yourDelegationsAddDelegationButton.setOnClickListener { viewModel.addDelegationClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .yourDelegationsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: YourDelegationsViewModel) {
        viewModel.delegateModels.observeWhenVisible {
            when (it) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loaded -> {
                    placeholderAdapter.show(false)
                    delegateListAdapter.submitListPreservingViewPoint(it.data, binder.yourDelegationsList)
                }
                ExtendedLoadingState.Loading -> {
                    placeholderAdapter.show(true)
                    delegateListAdapter.submitList(emptyList())
                }
            }
        }
    }

    override fun itemClicked(position: Int) {
        viewModel.delegateClicked(position)
    }
}
