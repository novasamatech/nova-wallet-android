package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start.StartSingleSelectStakingFragment

class StartParachainStakingFragment : StartSingleSelectStakingFragment<Collator, StartParachainStakingViewModel>() {

    companion object {

        private const val PAYLOAD = "StartParachainStakingFragment.Payload"

        fun getBundle(payload: StartParachainStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startParachainStakingFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }
}
