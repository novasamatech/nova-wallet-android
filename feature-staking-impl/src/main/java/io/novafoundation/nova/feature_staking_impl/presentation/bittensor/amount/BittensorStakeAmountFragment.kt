package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorStakeAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

class BittensorStakeAmountFragment : BaseFragment<BittensorStakeAmountViewModel, FragmentBittensorStakeAmountBinding>() {

    override fun createBinding() = FragmentBittensorStakeAmountBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.bittensorStakeAmountContinue.prepareForProgress(viewLifecycleOwner)
        binder.bittensorStakeAmountToolbar.setHomeButtonListener { viewModel.back() }
        binder.bittensorStakeAmountValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.bittensorStakeAmountContinue.setOnClickListener { viewModel.continueClicked() }
        binder.bittensorStakeAmount.amountInput.doAfterTextChanged { viewModel.amountChanged(it.toString()) }
        binder.bittensorStakeAmount.setMaxActionAvailability(
            MaxActionAvailability.Available {
                binder.bittensorStakeAmount.amountInput.requestFocus()
                viewModel.maxClicked()
            }
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorStakeAmountComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BittensorStakeAmountViewModel) {
        viewModel.amountModel.observe(binder.bittensorStakeAmount::setChooseAmountModel)
        viewModel.amount.observe(::showAmount)
        viewModel.fiatAmount.observe(binder.bittensorStakeAmount::setFiatAmount)
        viewModel.maxAmount.observe(binder.bittensorStakeAmount::setMaxAmountDisplay)
        viewModel.validator.observe(::showValidator)
        viewModel.apy.observe(binder.bittensorStakeAmountApy::showValue)
        viewModel.fee.observe(::showFee)
        viewModel.continueButtonState.observe(binder.bittensorStakeAmountContinue::setState)
    }

    private fun showAmount(amount: String) {
        with(binder.bittensorStakeAmount.amountInput) {
            if (text.toString() != amount) {
                setText(amount)
                setSelection(amount.length)
            }
        }
    }

    private fun showValidator(validator: BittensorValidatorUi) = with(binder) {
        bittensorStakeAmountValidator.showValue(validator.name)
        validator.icon?.let { bittensorStakeAmountValidator.loadImage(it) }
            ?: bittensorStakeAmountValidator.setImage(R.drawable.ic_nova, 20)
    }

    private fun showFee(fee: BittensorFeeUi) {
        binder.bittensorStakeAmountFee.showValue(fee.amount, fee.fiatAmount)
    }
}
