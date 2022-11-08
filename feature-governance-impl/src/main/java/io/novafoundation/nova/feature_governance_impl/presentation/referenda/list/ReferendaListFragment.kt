package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.PlaceholderAdapter
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetChange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetClick
import kotlinx.android.synthetic.main.fragment_referenda_list.referendaList
import javax.inject.Inject

class ReferendaListFragment : BaseFragment<ReferendaListViewModel>(), ReferendaListAdapter.Handler, ReferendaListHeaderAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val referendaHeaderAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListHeaderAdapter(imageLoader, this) }

    private val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_shimmering) }

    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_placeholder  ) }

    private val referendaListAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListAdapter(this, imageLoader) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referenda_list, container, false)
    }

    override fun initViews() {
        referendaList.itemAnimator = null
        referendaList.adapter = ConcatAdapter(referendaHeaderAdapter, shimmeringAdapter, placeholderAdapter, referendaListAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendaListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendaListViewModel) {
        subscribeOnAssetClick(viewModel.assetSelectorMixin, imageLoader)
        subscribeOnAssetChange(viewModel.assetSelectorMixin) {
            referendaHeaderAdapter.setAsset(it)
        }

        viewModel.governanceTotalLocks.observe {
            referendaHeaderAdapter.setLocks(it.dataOrNull)
        }

        viewModel.referendaUiFlow.observe {
            when (it) {
                is LoadingState.Loaded -> {
                    shimmeringAdapter.showPlaceholder(false)
                    referendaListAdapter.submitList(it.data)
                    placeholderAdapter.showPlaceholder(it.data.isEmpty())
                }
                is LoadingState.Loading -> {
                    shimmeringAdapter.showPlaceholder(true)
                    referendaListAdapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onReferendaClick(referendum: ReferendumModel) {
        viewModel.openReferendum(referendum)
    }

    override fun onClickAssetSelector() {
        viewModel.assetSelectorMixin.assetSelectorClicked()
    }

    override fun onClickGovernanceLocks() {
        viewModel.governanceLocksClicked()
    }
}
