package io.novafoundation.nova.feature_staking_impl.presentation.validators.current

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsFragment

class CurrentValidatorsFragment : CurrentStakeTargetsFragment<CurrentValidatorsViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .currentValidatorsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CurrentValidatorsViewModel) {
        super.subscribe(viewModel)
        observeValidations(viewModel)
    }
}
