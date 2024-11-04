package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.core.view.updateMarginsRelative
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentValidatorDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorAlert
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide

class ValidatorDetailsFragment : BaseFragment<ValidatorDetailsViewModel, FragmentValidatorDetailsBinding>() {

    companion object {
        private const val PAYLOAD = "ValidatorDetailsFragment.Payload"

        fun getBundle(payload: StakeTargetDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentValidatorDetailsBinding::bind)

    private val activeStakingFields by lazy(LazyThreadSafetyMode.NONE) {
        listOf(binder.validatorStakingStakers, binder.validatorStakingTotalStake, binder.validatorStakingEstimatedReward, binder.validatorStakingMinimumStake)
    }

    override fun initViews() {
        binder.validatorDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.validatorStakingTotalStake.setOnClickListener { viewModel.totalStakeClicked() }

        binder.validatorAccountInfo.setOnClickListener { viewModel.accountActionsClicked() }
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
        setupIdentityMixin(viewModel.identityMixin, binder.validatorIdentity)

        viewModel.stakeTargetDetails.observe { validator ->
            with(validator.stake) {
                binder.validatorStakingStatus.showValue(status.text)
                binder.validatorStakingStatus.setPrimaryValueEndIcon(status.icon, tint = status.iconTint)

                if (activeStakeModel != null) {
                    activeStakingFields.forEach(View::makeVisible)

                    binder.validatorStakingStakers.showValue(activeStakeModel.nominatorsCount, activeStakeModel.maxNominations)
                    binder.validatorStakingTotalStake.showAmount(activeStakeModel.totalStake)
                    binder.validatorStakingMinimumStake.showAmountOrHide(activeStakeModel.minimumStake)
                    binder.validatorStakingEstimatedReward.showValue(activeStakeModel.apy)
                } else {
                    activeStakingFields.forEach(View::makeGone)
                }
            }

            binder.validatorIdentity.setModelOrHide(validator.identity)

            binder.validatorAccountInfo.setAddressModel(validator.addressModel)
        }

        viewModel.errorFlow.observe { alerts ->
            removeAllAlerts()

            val alertViews = alerts.map(::createAlertView)
            binder.validatorDetailsContainer.addAfter(binder.validatorAccountInfo, alertViews)
        }

        viewModel.totalStakeEvent.observeEvent {
            ValidatorStakeBottomSheet(requireContext(), it).show()
        }

        binder.validatorDetailsToolbar.setTitle(viewModel.displayConfig.titleRes)
        binder.validatorStakingStakers.setTitle(viewModel.displayConfig.stakersLabelRes)
    }

    private fun removeAllAlerts() {
        binder.validatorDetailsContainer.children
            .filterIsInstance<AlertView>()
            .forEach(binder.validatorDetailsContainer::removeView)
    }

    private fun createAlertView(alert: ValidatorAlert): AlertView {
        val style = when (alert.severity) {
            ValidatorAlert.Severity.WARNING -> AlertView.StylePreset.WARNING
            ValidatorAlert.Severity.ERROR -> AlertView.StylePreset.ERROR
        }

        return AlertView(requireContext()).also { alertView ->
            alertView.setStylePreset(style)
            alertView.setMessage(alert.descriptionRes)

            alertView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also { params ->
                params.updateMarginsRelative(start = 16.dp, end = 16.dp, top = 12.dp)
            }
        }
    }
}
