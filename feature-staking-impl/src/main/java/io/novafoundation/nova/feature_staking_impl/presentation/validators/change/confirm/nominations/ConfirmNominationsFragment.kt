package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel
import kotlinx.android.synthetic.main.fragment_confirm_nominations.confirmNominationsList
import kotlinx.android.synthetic.main.fragment_confirm_nominations.confirmNominationsToolbar

class ConfirmNominationsFragment : BaseFragment<ConfirmNominationsViewModel>(), StakeTargetAdapter.ItemHandler<Validator> {

    lateinit var adapter: StakeTargetAdapter<Validator>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_nominations, container, false)
    }

    override fun initViews() {
        adapter = StakeTargetAdapter(this)
        confirmNominationsList.adapter = adapter

        confirmNominationsList.setHasFixedSize(true)

        confirmNominationsToolbar.setHomeButtonListener {
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

        viewModel.toolbarTitle.observe(confirmNominationsToolbar::setTitle)
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}
