package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel

class RecommendedValidatorsFragment : BaseFragment<RecommendedValidatorsViewModel>(), StakeTargetAdapter.ItemHandler<Validator> {

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommended_validators, container, false)
    }

    override fun initViews() {
        recommendedValidatorsList.adapter = adapter

        recommendedValidatorsList.setHasFixedSize(true)

        recommendedValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        recommendedValidatorsNext.setOnClickListener {
            viewModel.nextClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .recommendedValidatorsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: RecommendedValidatorsViewModel) {
        viewModel.recommendedValidatorModels.observe {
            adapter.submitList(it)

            recommendedValidatorsProgress.setVisible(false)
            recommendedValidatorsContent.setVisible(true)
        }

        viewModel.selectedTitle.observe(recommendedValidatorsAccounts::setText)
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}
