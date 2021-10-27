package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatDateFromMillis
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsContainer
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsDate
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsEra
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsReward
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsRewardFiat
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsSubmit
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsToolbar
import kotlinx.android.synthetic.main.fragment_payout_details.payoutDetailsValidator

class PayoutDetailsFragment : BaseFragment<PayoutDetailsViewModel>() {

    companion object {
        private const val KEY_PAYOUT = "validator"

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

        payoutDetailsValidator.setWholeClickListener { viewModel.validatorExternalActionClicked() }
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
            // TODO perform date formatting in viewModel
            payoutDetailsDate.text = it.createdAt.formatDateFromMillis(requireContext())
            payoutDetailsEra.text = it.eraDisplay
            payoutDetailsReward.text = it.reward
            payoutDetailsRewardFiat.text = it.rewardFiat

            payoutDetailsValidator.setMessage(it.validatorAddressModel.nameOrAddress)
            payoutDetailsValidator.setTextIcon(it.validatorAddressModel.image)
        }
    }
}
