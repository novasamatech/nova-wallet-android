package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorClearFilters
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorContentGroup
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorCount
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorList
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorProgress
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorSorting
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator.selectCollatorToolbar

abstract class SingleSelectChooseTargetFragment<T, V : SingleSelectChooseTargetViewModel<T, *>> : BaseFragment<V>(),
    StakeTargetAdapter.ItemHandler<T> {

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

        if (viewModel.searchVisible) {
            selectCollatorToolbar.addCustomAction(R.drawable.ic_search) {
                viewModel.searchClicked()
            }
        }

        selectCollatorList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        selectCollatorClearFilters.setOnClickListener { viewModel.clearFiltersClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        filterAction = null
    }

    override fun subscribe(viewModel: V) {
        viewModel.targetModelsFlow.observe {
            adapter.submitList(it)

            selectCollatorContentGroup.makeVisible()
            selectCollatorProgress.makeGone()
        }

        viewModel.targetsCount.observe(selectCollatorCount::setText)

        viewModel.scoringHeader.observe(selectCollatorSorting::setText)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }

        viewModel.clearFiltersEnabled.observe(selectCollatorClearFilters::setEnabled)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: StakeTargetModel<T>) {
        viewModel.targetInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: StakeTargetModel<T>) {
        viewModel.targetClicked(stakeTargetModel)
    }
}
