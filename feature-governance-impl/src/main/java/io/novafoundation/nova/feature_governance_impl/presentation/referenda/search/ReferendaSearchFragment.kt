package io.novafoundation.nova.feature_governance_impl.presentation.referenda.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.utils.applyImeInsetts
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.BaseReferendaListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel

class ReferendaSearchFragment : BaseReferendaListFragment<ReferendaSearchViewModel>() {

    override val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.item_referenda_shimmering_no_groups) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referenda_search, container, false)
    }

    override fun initViews() {
        searchReferendaToolbar.applyStatusBarInsets()
        view?.applyImeInsetts()

        searchedReferendaList.itemAnimator = null
        searchedReferendaList.adapter = ConcatAdapter(shimmeringAdapter, placeholderAdapter, referendaListAdapter)

        searchReferendaToolbar.cancel.setOnClickListener {
            viewModel.cancelClicked()
            view?.hideSoftKeyboard()
        }

        searchReferendaToolbar.searchInput.requestFocus()
        searchReferendaToolbar.searchInput.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendaSearchFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendaSearchViewModel) {
        searchReferendaToolbar.searchInput.content.bindTo(viewModel.queryFlow, lifecycleScope)

        viewModel.referendaUiFlow.observeReferendaList()
    }

    override fun submitReferenda(data: List<Any>) {
        referendaListAdapter.submitListPreservingViewPoint(
            data = data,
            into = searchedReferendaList,
            extraDiffCompletedCallback = { searchedReferendaList.invalidateItemDecorations() }
        )
    }

    override fun onReferendaClick(referendum: ReferendumModel) {
        viewModel.openReferendum(referendum)
    }
}
