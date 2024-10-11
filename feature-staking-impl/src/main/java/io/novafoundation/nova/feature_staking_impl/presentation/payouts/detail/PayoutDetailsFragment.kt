package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable

class PayoutDetailsFragment : BaseFragment<PayoutDetailsViewModel>() {

    companion object {
        private const val KEY_PAYOUT = "payout"

        fun getBundle(payout: PendingPayoutParcelable): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYOUT, payout)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_payout_details, container, false)
    }

    override fun initViews() {
        payoutDetailsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        payoutDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        payoutDetailsSubmit.setOnClickListener { viewModel.payoutClicked() }

        payoutDetailsValidator.setOnClickListener { viewModel.validatorExternalActionClicked() }
    }

    override fun inject() {
        val payout = argument<PendingPayoutParcelable>(KEY_PAYOUT)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .payoutDetailsFactory()
            .create(this, payout)
            .inject(this)
    }

    override fun subscribe(viewModel: PayoutDetailsViewModel) {
        setupExternalActions(viewModel)

        viewModel.payoutDetails.observe {
            payoutDetailsToolbar.titleView.startTimer(millis = it.timeLeft, millisCalculatedAt = it.timeLeftCalculatedAt)
            payoutDetailsToolbar.titleView.setTextColorRes(it.timerColor)

            payoutDetailsEra.showValue(it.eraDisplay)
            payoutDetailsValidator.showAddress(it.validatorAddressModel)

            payoutDetailsAmount.setAmount(it.reward)
        }
    }
}
