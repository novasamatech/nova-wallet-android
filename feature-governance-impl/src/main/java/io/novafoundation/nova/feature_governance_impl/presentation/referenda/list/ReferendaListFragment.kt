package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendaListBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.BaseReferendaListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetChange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetClick
import javax.inject.Inject

class ReferendaListFragment : BaseReferendaListFragment<ReferendaListViewModel, FragmentReferendaListBinding>(), ReferendaListHeaderAdapter.Handler {

    override fun createBinding() = FragmentReferendaListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val referendaHeaderAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListHeaderAdapter(imageLoader, this) }

    override fun initViews() {
        binder.referendaList.itemAnimator = null
        binder.referendaList.adapter = ConcatAdapter(referendaHeaderAdapter, shimmeringAdapter, placeholderAdapter, referendaListAdapter)
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
        observeValidations(viewModel)

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
