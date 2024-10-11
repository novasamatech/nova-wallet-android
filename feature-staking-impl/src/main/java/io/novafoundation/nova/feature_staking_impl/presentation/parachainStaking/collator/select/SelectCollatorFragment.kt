package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.CollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter

class SelectCollatorFragment : BaseFragment<SelectCollatorViewModel>(), StakeTargetAdapter.ItemHandler<Collator> {

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    private var filterAction: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_select_collator, container, false)
    }

    override fun initViews() {
        selectCollatorContainer.applyStatusBarInsets()

        selectCollatorList.adapter = adapter
        selectCollatorList.setHasFixedSize(true)
        selectCollatorList.itemAnimator = null

        selectCollatorToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        filterAction = selectCollatorToolbar.addCustomAction(R.drawable.ic_filter) {
            viewModel.settingsClicked()
        }

        selectCollatorToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        selectCollatorList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        selectCollatorClearFilters.setOnClickListener { viewModel.clearFiltersClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        filterAction = null
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCollatorFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCollatorViewModel) {
        viewModel.collatorModelsFlow.observe {
            adapter.submitList(it)

            selectCollatorContentGroup.makeVisible()
            selectCollatorProgress.makeGone()
        }

        viewModel.collatorsTitle.observe(selectCollatorCount::setText)

        viewModel.scoringHeader.observe(selectCollatorSorting::setText)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }

        viewModel.clearFiltersEnabled.observe(selectCollatorClearFilters::setEnabled)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: CollatorModel) {
        viewModel.collatorInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: CollatorModel) {
        viewModel.collatorClicked(stakeTargetModel)
    }
}
