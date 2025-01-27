package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingFragment

class ConfirmStartMythosStakingFragment : ConfirmStartSingleTargetStakingFragment<ConfirmStartMythosStakingViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmStartMythosStakingFragment.Payload"

        fun getBundle(payload: ConfirmStartMythosStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmStartMythosStakingFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }
}
