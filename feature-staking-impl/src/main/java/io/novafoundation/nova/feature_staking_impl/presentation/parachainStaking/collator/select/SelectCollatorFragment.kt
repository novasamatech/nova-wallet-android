package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import android.widget.ImageView

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingSelectCollatorBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.CollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter

class SelectCollatorFragment :
    BaseFragment<SelectCollatorViewModel, FragmentParachainStakingSelectCollatorBinding>(),
    StakeTargetAdapter.ItemHandler<Collator> {

    override fun createBinding() = FragmentParachainStakingSelectCollatorBinding.inflate(layoutInflater)

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    private var filterAction: ImageView? = null

    override fun initViews() {
        binder.selectCollatorContainer.applyStatusBarInsets()

        binder.selectCollatorList.adapter = adapter
        binder.selectCollatorList.setHasFixedSize(true)
        binder.selectCollatorList.itemAnimator = null

        binder.selectCollatorToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        filterAction = binder.selectCollatorToolbar.addCustomAction(R.drawable.ic_filter) {
            viewModel.settingsClicked()
        }

        binder.selectCollatorToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        binder.selectCollatorList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        binder.selectCollatorClearFilters.setOnClickListener { viewModel.clearFiltersClicked() }
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

            binder.selectCollatorContentGroup.makeVisible()
            binder.selectCollatorProgress.makeGone()
        }

        viewModel.collatorsTitle.observe(binder.selectCollatorCount::setText)

        viewModel.scoringHeader.observe(binder.selectCollatorSorting::setText)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }

        viewModel.clearFiltersEnabled.observe(binder.selectCollatorClearFilters::setEnabled)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: CollatorModel) {
        viewModel.collatorInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: CollatorModel) {
        viewModel.collatorClicked(stakeTargetModel)
    }
}
