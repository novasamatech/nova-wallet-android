package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SingleSelectChooseTargetFragment

class SelectCollatorFragment : SingleSelectChooseTargetFragment<Collator, SelectCollatorViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCollatorFactory()
            .create(this)
            .inject(this)
    }
}
