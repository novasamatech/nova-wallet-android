package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorStakeConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi

private const val PAYLOAD_KEY = "payload"

class BittensorStakeConfirmFragment : BaseFragment<BittensorStakeConfirmViewModel, FragmentBittensorStakeConfirmBinding>() {

    companion object {

        fun getBundle(payload: BittensorStakeConfirmPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentBittensorStakeConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.bittensorStakeConfirmButton.prepareForProgress(viewLifecycleOwner)
        binder.bittensorStakeConfirmToolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        binder.bittensorStakeConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.accountClicked() }
        binder.bittensorStakeConfirmButton.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<BittensorStakeConfirmPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorStakeConfirmComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: BittensorStakeConfirmViewModel) {
        observeValidations(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        setupExternalActions(viewModel)

        viewModel.amount.observe(binder.bittensorStakeConfirmAmount::setAmount)
        viewModel.wallet.observe(binder.bittensorStakeConfirmExtrinsicInformation::setWallet)
        viewModel.account.observe(binder.bittensorStakeConfirmExtrinsicInformation::setAccount)
        viewModel.fee.observe(binder.bittensorStakeConfirmExtrinsicInformation::setFeeStatus)
        binder.bittensorStakeConfirmStakingType.showValue(viewModel.stakingType)
        viewModel.validator.observe(::showValidator)
        viewModel.stakeAfter.observe(binder.stakeAfter::showValue)
        viewModel.apy.observe(binder.bittensorStakeConfirmApy::showValue)
        viewModel.confirmButtonState.observe(binder.bittensorStakeConfirmButton::setState)
        bindBittensorStakeResults(
            results = viewModel.resultState,
            onDismiss = viewModel::resultDismissed,
            onTryAgain = viewModel::confirmClicked,
            onViewPosition = viewModel::viewPositionClicked
        )
    }

    private fun showValidator(validator: BittensorValidatorUi) {
        binder.bittensorStakeConfirmValidator.showValue(validator.name)
        validator.icon?.let { binder.bittensorStakeConfirmValidator.loadImage(it) }
            ?: binder.bittensorStakeConfirmValidator.setImage(R.drawable.ic_nova, 20)
    }
}
