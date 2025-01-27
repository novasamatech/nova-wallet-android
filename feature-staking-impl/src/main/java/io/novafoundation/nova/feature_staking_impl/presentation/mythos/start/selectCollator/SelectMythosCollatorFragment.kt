package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SingleSelectChooseTargetFragment

class SelectMythosCollatorFragment : SingleSelectChooseTargetFragment<MythosCollator, SelectMythosCollatorViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectMythosCollatorFactory()
            .create(this)
            .inject(this)
    }
}
