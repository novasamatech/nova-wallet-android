package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentPayoutDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable

class PayoutDetailsFragment : BaseFragment<PayoutDetailsViewModel, FragmentPayoutDetailsBinding>() {

    companion object {
        private const val KEY_PAYOUT = "payout"

        fun getBundle(payout: PendingPayoutParcelable): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYOUT, payout)
            }
        }
    }

    override val binder by viewBinding(FragmentPayoutDetailsBinding::bind)

    override fun initViews() {
        binder.payoutDetailsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        binder.payoutDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.payoutDetailsSubmit.setOnClickListener { viewModel.payoutClicked() }

        binder.payoutDetailsValidator.setOnClickListener { viewModel.validatorExternalActionClicked() }
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
            binder.payoutDetailsToolbar.titleView.startTimer(millis = it.timeLeft, millisCalculatedAt = it.timeLeftCalculatedAt)
            binder.payoutDetailsToolbar.titleView.setTextColorRes(it.timerColor)

            binder.payoutDetailsEra.showValue(it.eraDisplay)
            binder.payoutDetailsValidator.showAddress(it.validatorAddressModel)

            binder.payoutDetailsAmount.setAmount(it.reward)
        }
    }
}
