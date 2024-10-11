package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmPayoutFragment : BaseFragment<ConfirmPayoutViewModel>() {

    companion object {
        private const val KEY_PAYOUTS = "payouts"

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
        confirmPayoutContainer.applyStatusBarInsets()

        confirmPayoutConfirm.setOnClickListener { viewModel.submitClicked() }
        confirmPayoutConfirm.prepareForProgress(viewLifecycleOwner)

        confirmPayoutToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmPayoutExtrinsicInformation.setOnAccountClickedListener { viewModel.accountClicked() }
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
        observeRetries(viewModel.partialRetriableMixin)
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        observeRetries(viewModel)
        setupFeeLoading(viewModel, confirmPayoutExtrinsicInformation.fee)

        viewModel.initiatorAddressModel.observe(confirmPayoutExtrinsicInformation::setAccount)
        viewModel.walletUiFlow.observe(confirmPayoutExtrinsicInformation::setWallet)

        viewModel.totalRewardFlow.observe(confirmPayoutAmount::setAmount)

        viewModel.showNextProgress.observe(confirmPayoutConfirm::setProgressState)
    }
}
