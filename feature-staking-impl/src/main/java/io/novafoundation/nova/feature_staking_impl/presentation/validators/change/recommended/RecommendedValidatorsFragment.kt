package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentRecommendedValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel

class RecommendedValidatorsFragment :
    BaseFragment<RecommendedValidatorsViewModel, FragmentRecommendedValidatorsBinding>(),
    StakeTargetAdapter.ItemHandler<Validator> {

    override fun createBinding() = FragmentRecommendedValidatorsBinding.inflate(layoutInflater)

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override fun initViews() {
        binder.recommendedValidatorsList.adapter = adapter

        binder.recommendedValidatorsList.setHasFixedSize(true)

        binder.recommendedValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.recommendedValidatorsNext.setOnClickListener {
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

            binder.recommendedValidatorsProgress.setVisible(false)
            binder.recommendedValidatorsContent.setVisible(true)
        }

        viewModel.selectedTitle.observe(binder.recommendedValidatorsAccounts::setText)
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}
