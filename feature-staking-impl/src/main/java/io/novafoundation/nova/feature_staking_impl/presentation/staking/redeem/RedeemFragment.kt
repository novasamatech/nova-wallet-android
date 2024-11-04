package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentRedeemBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class RedeemFragment : BaseFragment<RedeemViewModel, FragmentRedeemBinding>() {

    companion object {

        fun getBundle(payload: RedeemPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override val binder by viewBinding(FragmentRedeemBinding::bind)

    override fun initViews() {
        binder.redeemContainer.applyStatusBarInsets()

        binder.redeemToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.redeemConfirm.prepareForProgress(viewLifecycleOwner)
        binder.redeemConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.redeemExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }
    }

    override fun inject() {
        val payload = argument<RedeemPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .redeemFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: RedeemViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, binder.redeemExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(binder.redeemConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.redeemAmount::setAmount)

        viewModel.walletUiFlow.observe(binder.redeemExtrinsicInformation::setWallet)
        viewModel.feeLiveData.observe(binder.redeemExtrinsicInformation::setFeeStatus)
        viewModel.originAddressModelFlow.observe(binder.redeemExtrinsicInformation::setAccount)
    }
}
