package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.PoolStakeTargetModel
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolList
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolRecommendedAction
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolToolbar
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsContainer
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsCount
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsFillWithRecommended

class SelectCustomPoolFragment : BaseFragment<SelectCustomPoolViewModel>(), StakeTargetAdapter.ItemHandler<NominationPool> {

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_custom_pool, container, false)
    }

    override fun initViews() {
        selectCustomValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        selectCustomPoolToolbar.setHomeButtonListener { viewModel.backClicked() }
        selectCustomPoolToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        selectCustomPoolList.adapter = adapter
        selectCustomPoolList.setHasFixedSize(true)
        selectCustomPoolList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        selectCustomPoolRecommendedAction.setOnClickListener { viewModel.selectRecommended() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCustomPoolComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCustomPoolViewModel) {
        viewModel.poolModelsFlow.observe(adapter::submitList)

        viewModel.selectedTitle.observe(selectCustomValidatorsCount::setText)

        viewModel.fillWithRecommendedEnabled.observe(selectCustomValidatorsFillWithRecommended::setEnabled)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: PoolStakeTargetModel) {
        viewModel.poolInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: PoolStakeTargetModel) {
        viewModel.poolClicked(stakeTargetModel)
    }
}
