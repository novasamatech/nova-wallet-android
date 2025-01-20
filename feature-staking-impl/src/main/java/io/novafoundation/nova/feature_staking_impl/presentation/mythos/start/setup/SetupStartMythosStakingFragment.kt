package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start.StartSingleSelectStakingFragment

class SetupStartMythosStakingFragment : StartSingleSelectStakingFragment<MythosCollator, SetupStartMythosStakingViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startMythosStakingFactory()
            .create(this)
            .inject(this)
    }
}
