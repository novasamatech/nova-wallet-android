package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter.DelegateListAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_your_delegations.yourDelegationsAddDelegationButton
import kotlinx.android.synthetic.main.fragment_your_delegations.yourDelegationsList
import kotlinx.android.synthetic.main.fragment_your_delegations.yourDelegationsToolbar

class YourDelegationsFragment :
    BaseFragment<YourDelegationsViewModel>(),
    DelegateListAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.item_delegates_shimmering) }
    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(placeholderAdapter, delegateListAdapter) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_your_delegations, container, false)
    }

    override fun initViews() {
        yourDelegationsList.itemAnimator = null
        yourDelegationsList.adapter = adapter

        yourDelegationsToolbar.applyStatusBarInsets()
        yourDelegationsToolbar.setHomeButtonListener { viewModel.backClicked() }
        yourDelegationsAddDelegationButton.setOnClickListener { viewModel.addDelegationClicked() }
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
                    delegateListAdapter.submitListPreservingViewPoint(it.data, yourDelegationsList)
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
