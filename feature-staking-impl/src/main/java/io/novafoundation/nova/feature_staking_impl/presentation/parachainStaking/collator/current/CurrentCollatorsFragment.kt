package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.actions.CollatorManageActionsBottomSheet

class CurrentCollatorsFragment : CurrentStakeTargetsFragment<CurrentCollatorsViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .currentCollatorsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CurrentCollatorsViewModel) {
        super.subscribe(viewModel)

        viewModel.selectManageCollatorsAction.awaitableActionLiveData.observeEvent {
            CollatorManageActionsBottomSheet(
                context = requireContext(),
                itemSelected = it.onSuccess,
                onCancel = it.onCancel
            ).show()
        }
    }
}
