package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.search.SearchStakeTargetFragment

class SearchCustomValidatorsFragment : SearchStakeTargetFragment<SearchCustomValidatorsViewModel, Validator>() {

    override val configuration by lazy(LazyThreadSafetyMode.NONE) {
        Configuration(
            doneAction = viewModel::doneClicked,
            sortingLabelRes = R.string.staking_rewards_apy
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .searchCustomValidatorsComponentFactory()
            .create(this)
            .inject(this)
    }
}
