package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.sendEmailIntent
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel
import kotlinx.android.synthetic.main.fragment_validator_details.validatorAccountInfo
import kotlinx.android.synthetic.main.fragment_validator_details.validatorDetailsToolbar
import kotlinx.android.synthetic.main.fragment_validator_details.validatorIdentity
import kotlinx.android.synthetic.main.fragment_validator_details.validatorInfo

class ValidatorDetailsFragment : BaseFragment<ValidatorDetailsViewModel>() {

    companion object {
        private const val KEY_VALIDATOR = "validator"

        fun getBundle(validator: ValidatorDetailsParcelModel): Bundle {
            return Bundle().apply {
                putParcelable(KEY_VALIDATOR, validator)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_validator_details, container, false)
    }

    override fun initViews() {
        validatorDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        validatorInfo.setTotalStakeClickListener {
            viewModel.totalStakeClicked()
        }

        validatorIdentity.setEmailClickListener {
            viewModel.emailClicked()
        }

        validatorIdentity.setWebClickListener {
            viewModel.webClicked()
        }

        validatorIdentity.setTwitterClickListener {
            viewModel.twitterClicked()
        }

        validatorAccountInfo.setWholeClickListener { viewModel.accountActionsClicked() }
    }

    override fun inject() {
        val validator = argument<ValidatorDetailsParcelModel>(KEY_VALIDATOR)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .validatorDetailsComponentFactory()
            .create(this, validator)
            .inject(this)
    }

    override fun subscribe(viewModel: ValidatorDetailsViewModel) {
        setupExternalActions(viewModel)

        viewModel.validatorDetails.observe { validator ->
            with(validator.stake) {
                validatorInfo.setStatus(statusText, statusColorRes)

                if (activeStakeModel != null) {
                    validatorInfo.showActiveStakeFields()

                    validatorInfo.setNominatorsCount(activeStakeModel.nominatorsCount, activeStakeModel.maxNominations)
                    validatorInfo.setEstimatedRewardApy(activeStakeModel.apy)
                    validatorInfo.setTotalStakeValue(activeStakeModel.totalStake)
                    validatorInfo.setTotalStakeValueFiat(activeStakeModel.totalStakeFiat)
                } else {
                    validatorInfo.hideActiveStakeFields()
                }
            }

            if (validator.identity == null) {
                validatorIdentity.makeGone()
            } else {
                validatorIdentity.makeVisible()
                validatorIdentity.populateIdentity(validator.identity)
            }

            validatorAccountInfo.setAccountIcon(validator.addressImage)

            if (validator.identity?.display == null) {
                validatorAccountInfo.setTitle(validator.address)
                validatorAccountInfo.hideBody()
            } else {
                validatorAccountInfo.setTitle(validator.identity.display)
                validatorAccountInfo.setText(validator.address)
                validatorAccountInfo.showBody()
            }
        }

        viewModel.errorFlow.observe {
            it?.let { validatorInfo.setErrors(it) }
        }

        viewModel.openEmailEvent.observeEvent {
            requireContext().sendEmailIntent(it)
        }

        viewModel.totalStakeEvent.observeEvent {
            ValidatorStakeBottomSheet(requireContext(), it).show()
        }
    }
}
