package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search

import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentDelegateSearchBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter.DelegateListAdapter
import javax.inject.Inject

class DelegateSearchFragment :
    BaseFragment<DelegateSearchViewModel, FragmentDelegateSearchBinding>(),
    DelegateListAdapter.Handler {

    override val binder by viewBinding(FragmentDelegateSearchBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val delegateSearchCountResultAdapter = DelegateSearchCountResultAdapter()
    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(delegateSearchCountResultAdapter, delegateListAdapter) }

    override fun initViews() {
        binder.delegateSearchList.itemAnimator = null
        binder.delegateSearchList.setHasFixedSize(true)
        binder.delegateSearchList.adapter = adapter

        binder.delegateSearchNavigation.applyStatusBarInsets()
        binder.delegateSearchToolbar.setHomeButtonListener {
            viewModel.backClicked()
            hideKeyboard()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .delegateSearchFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DelegateSearchViewModel) {
        binder.delegateSearchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchPlaceholderModel.observeWhenVisible {
            binder.delegateSearchPlaceholder.isGone = it == null
            if (it != null) {
                binder.delegateSearchPlaceholder.setImage(it.drawableRes)
                binder.delegateSearchPlaceholder.setText(it.textRes)
            }
        }

        viewModel.searchResultCount.observeWhenVisible {
            delegateSearchCountResultAdapter.setSearchResultCount(it)
        }

        viewModel.delegateModels.observeWhenVisible {
            binder.delegateSearchList.isGone = it is ExtendedLoadingState.Loading
            binder.delegateSearchProgressBar.isVisible = it is ExtendedLoadingState.Loading

            if (it is ExtendedLoadingState.Loaded) {
                delegateListAdapter.submitList(it.data)
            }
        }
    }

    override fun itemClicked(position: Int) {
        viewModel.delegateClicked(position)
    }
}
