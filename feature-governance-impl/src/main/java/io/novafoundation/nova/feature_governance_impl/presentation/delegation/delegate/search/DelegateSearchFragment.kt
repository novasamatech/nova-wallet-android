package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter.DelegateListAdapter
import javax.inject.Inject

class DelegateSearchFragment :
    BaseFragment<DelegateSearchViewModel>(),
    DelegateListAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val delegateSearchCountResultAdapter = DelegateSearchCountResultAdapter()
    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(delegateSearchCountResultAdapter, delegateListAdapter) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_delegate_search, container, false)
    }

    override fun initViews() {
        delegateSearchList.itemAnimator = null
        delegateSearchList.setHasFixedSize(true)
        delegateSearchList.adapter = adapter

        delegateSearchNavigation.applyStatusBarInsets()
        delegateSearchToolbar.setHomeButtonListener {
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
        delegateSearchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchPlaceholderModel.observeWhenVisible {
            delegateSearchPlaceholder.isGone = it == null
            if (it != null) {
                delegateSearchPlaceholder.setImage(it.drawableRes)
                delegateSearchPlaceholder.setText(it.textRes)
            }
        }

        viewModel.searchResultCount.observeWhenVisible {
            delegateSearchCountResultAdapter.setSearchResultCount(it)
        }

        viewModel.delegateModels.observeWhenVisible {
            delegateSearchList.isGone = it is ExtendedLoadingState.Loading
            delegateSearchProgressBar.isVisible = it is ExtendedLoadingState.Loading

            if (it is ExtendedLoadingState.Loaded) {
                delegateListAdapter.submitList(it.data)
            }
        }
    }

    override fun itemClicked(position: Int) {
        viewModel.delegateClicked(position)
    }
}
