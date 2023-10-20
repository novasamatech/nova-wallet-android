package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter.Mode.EDIT
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter.Mode.VIEW
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import kotlinx.android.synthetic.main.fragment_review_custom_validators.reviewCustomValidatorsAccounts
import kotlinx.android.synthetic.main.fragment_review_custom_validators.reviewCustomValidatorsList
import kotlinx.android.synthetic.main.fragment_review_custom_validators.reviewCustomValidatorsNext
import kotlinx.android.synthetic.main.fragment_review_custom_validators.reviewCustomValidatorsToolbar

class ReviewCustomValidatorsFragment : BaseFragment<ReviewCustomValidatorsViewModel>(), StakeTargetAdapter.ItemHandler<Validator> {

    companion object {

        private const val KEY_PAYLOAD = "SelectCustomValidatorsFragment.Payload"

        fun getBundle(
            payload: CustomValidatorsPayload
        ) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_review_custom_validators, container, false)
    }

    override fun initViews() {
        reviewCustomValidatorsList.adapter = adapter

        reviewCustomValidatorsList.setHasFixedSize(true)

        reviewCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        reviewCustomValidatorsNext.setOnClickListener {
            viewModel.nextClicked()
        }

        reviewCustomValidatorsToolbar.setRightActionClickListener {
            viewModel.isInEditMode.toggle()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .reviewCustomValidatorsComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ReviewCustomValidatorsViewModel) {
        viewModel.selectedValidatorModels.observe(adapter::submitList)

        viewModel.selectionStateFlow.observe {
            reviewCustomValidatorsAccounts.setTextColorRes(if (it.isOverflow) R.color.text_negative else R.color.text_primary)
            reviewCustomValidatorsAccounts.text = it.selectedHeaderText

            reviewCustomValidatorsNext.setState(if (it.isOverflow) ButtonState.DISABLED else ButtonState.NORMAL)
            reviewCustomValidatorsNext.text = it.nextButtonText
        }

        viewModel.isInEditMode.observe {
            adapter.modeChanged(if (it) EDIT else VIEW)

            val rightActionRes = if (it) R.string.common_done else R.string.common_edit

            reviewCustomValidatorsToolbar.setTextRight(getString(rightActionRes))
        }
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun removeClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.deleteClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}
