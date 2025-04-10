package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget

import android.widget.ImageView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingSelectCollatorBinding
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel

abstract class SingleSelectChooseTargetFragment<T, V : SingleSelectChooseTargetViewModel<T, *>> :
    BaseFragment<V, FragmentParachainStakingSelectCollatorBinding>(),
    StakeTargetAdapter.ItemHandler<T> {

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

        if (viewModel.searchVisible) {
            binder.selectCollatorToolbar.addCustomAction(R.drawable.ic_search) {
                viewModel.searchClicked()
            }
        }

        binder.selectCollatorList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        binder.selectCollatorClearFilters.setOnClickListener { viewModel.clearFiltersClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        filterAction = null
    }

    override fun subscribe(viewModel: V) {
        viewModel.targetModelsFlow.observe {
            adapter.submitList(it)

            binder.selectCollatorContentGroup.makeVisible()
            binder.selectCollatorProgress.makeGone()
        }

        viewModel.targetsCount.observe(binder.selectCollatorCount::setText)

        viewModel.scoringHeader.observe(binder.selectCollatorSorting::setText)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }

        viewModel.clearFiltersEnabled.observe(binder.selectCollatorClearFilters::setEnabled)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: StakeTargetModel<T>) {
        viewModel.targetInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: StakeTargetModel<T>) {
        viewModel.targetClicked(stakeTargetModel)
    }
}
