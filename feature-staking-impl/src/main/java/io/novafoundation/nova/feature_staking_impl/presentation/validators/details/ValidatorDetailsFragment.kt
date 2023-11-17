package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.core.view.updateMarginsRelative
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.setModelOrHide
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.setupIdentityMixin
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorAlert
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide
import kotlinx.android.synthetic.main.fragment_validator_details.validatorAccountInfo
import kotlinx.android.synthetic.main.fragment_validator_details.validatorDetailsContainer
import kotlinx.android.synthetic.main.fragment_validator_details.validatorDetailsToolbar
import kotlinx.android.synthetic.main.fragment_validator_details.validatorIdentity
import kotlinx.android.synthetic.main.fragment_validator_details.validatorStakingEstimatedReward
import kotlinx.android.synthetic.main.fragment_validator_details.validatorStakingMinimumStake
import kotlinx.android.synthetic.main.fragment_validator_details.validatorStakingStakers
import kotlinx.android.synthetic.main.fragment_validator_details.validatorStakingStatus
import kotlinx.android.synthetic.main.fragment_validator_details.validatorStakingTotalStake

class ValidatorDetailsFragment : BaseFragment<ValidatorDetailsViewModel>() {

    companion object {
        private const val PAYLOAD = "ValidatorDetailsFragment.Payload"

        fun getBundle(payload: StakeTargetDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
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

    private val activeStakingFields by lazy(LazyThreadSafetyMode.NONE) {
        listOf(validatorStakingStakers, validatorStakingTotalStake, validatorStakingEstimatedReward, validatorStakingMinimumStake)
    }

    override fun initViews() {
        validatorDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        validatorStakingTotalStake.setOnClickListener { viewModel.totalStakeClicked() }

        validatorAccountInfo.setOnClickListener { viewModel.accountActionsClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .validatorDetailsComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ValidatorDetailsViewModel) {
        setupExternalActions(viewModel)
        setupIdentityMixin(viewModel.identityMixin, validatorIdentity)

        viewModel.stakeTargetDetails.observe { validator ->
            with(validator.stake) {
                validatorStakingStatus.showValue(status.text)
                validatorStakingStatus.setPrimaryValueEndIcon(status.icon, tint = status.iconTint)

                if (activeStakeModel != null) {
                    activeStakingFields.forEach(View::makeVisible)

                    validatorStakingStakers.showValue(activeStakeModel.nominatorsCount, activeStakeModel.maxNominations)
                    validatorStakingTotalStake.showAmount(activeStakeModel.totalStake)
                    validatorStakingMinimumStake.showAmountOrHide(activeStakeModel.minimumStake)
                    validatorStakingEstimatedReward.showValue(activeStakeModel.apy)
                } else {
                    activeStakingFields.forEach(View::makeGone)
                }
            }

            validatorIdentity.setModelOrHide(validator.identity)

            validatorAccountInfo.setAddressModel(validator.addressModel)
        }

        viewModel.errorFlow.observe { alerts ->
            removeAllAlerts()

            val alertViews = alerts.map(::createAlertView)
            validatorDetailsContainer.addAfter(validatorAccountInfo, alertViews)
        }

        viewModel.totalStakeEvent.observeEvent {
            ValidatorStakeBottomSheet(requireContext(), it).show()
        }

        validatorDetailsToolbar.setTitle(viewModel.displayConfig.titleRes)
        validatorStakingStakers.setTitle(viewModel.displayConfig.stakersLabelRes)
    }

    private fun removeAllAlerts() {
        validatorDetailsContainer.children
            .filterIsInstance<AlertView>()
            .forEach(validatorDetailsContainer::removeView)
    }

    private fun createAlertView(alert: ValidatorAlert): AlertView {
        val style = when (alert.severity) {
            ValidatorAlert.Severity.WARNING -> AlertView.StylePreset.WARNING
            ValidatorAlert.Severity.ERROR -> AlertView.StylePreset.ERROR
        }

        return AlertView(requireContext()).also { alertView ->
            alertView.setStylePreset(style)
            alertView.setText(alert.descriptionRes)

            alertView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also { params ->
                params.updateMarginsRelative(start = 16.dp, end = 16.dp, top = 12.dp)
            }
        }
    }
}
