package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.BaseReferendaListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetChange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetClick
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_referenda_list.referendaList

class ReferendaListFragment : BaseReferendaListFragment<ReferendaListViewModel>(), ReferendaListHeaderAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val referendaHeaderAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListHeaderAdapter(imageLoader, this) }

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

        viewModel.governanceTotalLocks.observeWhenVisible {
            referendaHeaderAdapter.setLocks(it.dataOrNull)
        }

        viewModel.governanceDelegated.observeWhenVisible {
            referendaHeaderAdapter.setDelegations(it.dataOrNull)
        }

        viewModel.tinderGovBanner.observeWhenVisible {
            referendaHeaderAdapter.setTinderGovBanner(it)
        }

        viewModel.referendaFilterIcon.observeWhenVisible {
            referendaHeaderAdapter.setFilterIcon(it)
        }

        viewModel.referendaUiFlow.observeReferendaList()
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

    override fun onClickDelegations() {
        viewModel.delegationsClicked()
    }

    override fun onClickReferendaSearch() {
        viewModel.searchClicked()
    }

    override fun onClickReferendaFilters() {
        viewModel.filtersClicked()
    }

    override fun onTinderGovBannerClicked() {
        viewModel.openTinderGovCards()
    }
}
