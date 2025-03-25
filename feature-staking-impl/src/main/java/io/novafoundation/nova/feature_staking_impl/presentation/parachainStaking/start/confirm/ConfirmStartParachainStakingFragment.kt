package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload

class ConfirmStartParachainStakingFragment : ConfirmStartSingleTargetStakingFragment<ConfirmStartParachainStakingViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmStartParachainStakingFragment.Payload"

        fun getBundle(payload: ConfirmStartParachainStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmStartParachainStakingFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }
}
