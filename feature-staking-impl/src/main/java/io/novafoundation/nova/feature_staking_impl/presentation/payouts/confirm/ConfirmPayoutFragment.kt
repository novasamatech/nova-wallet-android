package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeViews
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.displayFeeStatus
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutConfirm
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutContainer
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutFeeFiat
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutFeeProgress
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutFeeToken
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutOriginAccount
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutRewardDestination
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutRewardFiat
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutRewardToken
import kotlinx.android.synthetic.main.fragment_confirm_payout.confirmPayoutToolbar

class ConfirmPayoutFragment : BaseFragment<ConfirmPayoutViewModel>() {

    companion object {
        private const val KEY_PAYOUTS = "validator"

        fun getBundle(payload: ConfirmPayoutPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYOUTS, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_payout, container, false)
    }

    override fun initViews() {
        confirmPayoutContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        confirmPayoutConfirm.setOnClickListener { viewModel.submitClicked() }

        confirmPayoutToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmPayoutConfirm.prepareForProgress(viewLifecycleOwner)

        confirmPayoutOriginAccount.setWholeClickListener { viewModel.controllerClicked() }
        confirmPayoutRewardDestination.setWholeClickListener { viewModel.rewardDestinationClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmPayoutPayload>(KEY_PAYOUTS)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmPayoutFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmPayoutViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        observeRetries(viewModel)

        viewModel.feeLiveData.observe {
            displayFeeStatus(it, FeeViews(confirmPayoutFeeProgress, confirmPayoutFeeFiat, confirmPayoutFeeToken))
        }

        viewModel.initiatorAddressModel.observe {
            with(confirmPayoutOriginAccount) {
                setMessage(it.nameOrAddress)
                setTextIcon(it.image)
            }
        }

        viewModel.rewardDestinationModel.observe {
            with(confirmPayoutRewardDestination) {
                setMessage(it.nameOrAddress)
                setTextIcon(it.image)
            }
        }

        viewModel.totalRewardDisplay.observe { (inToken, inFiat) ->
            confirmPayoutRewardToken.text = inToken
            confirmPayoutRewardFiat.text = inFiat
        }

        viewModel.showNextProgress.observe(confirmPayoutConfirm::setProgress)
    }
}
