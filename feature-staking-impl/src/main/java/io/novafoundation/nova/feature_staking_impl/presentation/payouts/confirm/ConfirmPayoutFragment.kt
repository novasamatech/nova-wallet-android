package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmPayoutBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmPayoutFragment : BaseFragment<ConfirmPayoutViewModel, FragmentConfirmPayoutBinding>() {

    companion object {
        private const val KEY_PAYOUTS = "payouts"

        fun getBundle(payload: ConfirmPayoutPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYOUTS, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentConfirmPayoutBinding::bind)

    override fun initViews() {
        binder.confirmPayoutContainer.applyStatusBarInsets()

        binder.confirmPayoutConfirm.setOnClickListener { viewModel.submitClicked() }
        binder.confirmPayoutConfirm.prepareForProgress(viewLifecycleOwner)

        binder.confirmPayoutToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.confirmPayoutExtrinsicInformation.setOnAccountClickedListener { viewModel.accountClicked() }
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
        setupFeeLoading(viewModel, binder.confirmPayoutExtrinsicInformation.fee)

        viewModel.initiatorAddressModel.observe(binder.confirmPayoutExtrinsicInformation::setAccount)
        viewModel.walletUiFlow.observe(binder.confirmPayoutExtrinsicInformation::setWallet)

        viewModel.totalRewardFlow.observe(binder.confirmPayoutAmount::setAmount)

        viewModel.showNextProgress.observe(binder.confirmPayoutConfirm::setProgressState)
    }
}
