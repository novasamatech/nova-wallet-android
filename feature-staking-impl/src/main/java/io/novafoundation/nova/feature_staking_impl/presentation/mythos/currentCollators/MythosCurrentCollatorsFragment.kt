package io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.actions.CollatorManageActionsBottomSheet

class MythosCurrentCollatorsFragment : CurrentStakeTargetsFragment<MythosCurrentCollatorsViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .currentMythosCollatorsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MythosCurrentCollatorsViewModel) {
        super.subscribe(viewModel)

        viewModel.selectManageCurrentStakeTargetsAction.awaitableActionLiveData.observeEvent {
            CollatorManageActionsBottomSheet(
                context = requireContext(),
                itemSelected = it.onSuccess,
                onCancel = it.onCancel
            ).show()
        }
    }
}
