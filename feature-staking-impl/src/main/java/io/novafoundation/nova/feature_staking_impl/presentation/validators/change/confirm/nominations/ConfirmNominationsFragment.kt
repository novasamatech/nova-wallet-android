package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmNominationsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel

class ConfirmNominationsFragment : BaseFragment<ConfirmNominationsViewModel, FragmentConfirmNominationsBinding>(), StakeTargetAdapter.ItemHandler<Validator> {

    lateinit var adapter: StakeTargetAdapter<Validator>

    override val binder by viewBinding(FragmentConfirmNominationsBinding::bind)

    override fun initViews() {
        adapter = StakeTargetAdapter(this)
        binder.confirmNominationsList.adapter = adapter

        binder.confirmNominationsList.setHasFixedSize(true)

        binder.confirmNominationsToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmNominationsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmNominationsViewModel) {
        viewModel.selectedValidatorsLiveData.observe(adapter::submitList)

        viewModel.toolbarTitle.observe(binder.confirmNominationsToolbar::setTitle)
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}
