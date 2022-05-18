package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.common.search.SearchStakeTargetFragment

class SearchCollatorFragment : SearchStakeTargetFragment<SearchCollatorViewModel, Collator>() {

    override val configuration by lazy(LazyThreadSafetyMode.NONE) {
        Configuration(doneAction = null, sortingLabelRes = R.string.staking_rewards)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .searchCollatorFactory()
            .create(this)
            .inject(this)
    }
}
