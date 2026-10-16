package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.view.showLoadingValue
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorExitAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

class BittensorExitAmountFragment : BaseFragment<BittensorExitAmountViewModel, FragmentBittensorExitAmountBinding>() {

    companion object {

        private const val PAYLOAD = "BittensorExitAmountFragment.Payload"

        fun getBundle(payload: BittensorExitPositionPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentBittensorExitAmountBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorExitAmountComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        amount.setTitle(getString(if (viewModel.isRoot) R.string.bittensor_exit_you_unstake else R.string.bittensor_exit_you_sell))
        amount.amountInput.doAfterTextChanged { viewModel.amountChanged(it.toString()) }
        amount.setMaxActionAvailability(MaxActionAvailability.Available { viewModel.maxClicked() })
        receive.isVisible = !viewModel.isRoot
        rate.isVisible = !viewModel.isRoot
        averagePrice.isVisible = !viewModel.isRoot
        earned.isVisible = !viewModel.isRoot
        validator.image.updateLayoutParams { width = 18.dp(requireContext()); height = 18.dp(requireContext()) }
        validator.setOnClickListener { viewModel.validatorClicked() }
        retry.setOnClickListener { viewModel.retry() }
        continueButton.prepareForProgress(viewLifecycleOwner)
        continueButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun subscribe(viewModel: BittensorExitAmountViewModel) = with(binder) {
        setupExternalActions(viewModel)
        viewModel.title.observe(toolbar::setTitle)
        viewModel.amountModel.observe { it?.let(amount::setChooseAmountModel) }
        viewModel.amount.observe { if (amount.amountInput.text.toString() != it) amount.amountInput.setText(it) }
        viewModel.maxAmount.observe(amount::setMaxAmountDisplay)
        viewModel.fiatAmount.observe(amount::setFiatAmount)
        validator.showValue(viewModel.validator)
        viewModel.averagePrice.observe(averagePrice::showLoadingValue)
        viewModel.earned.observe(earned::showLoadingValue)
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
        }
        viewModel.received.observe(receive::showValue)
        viewModel.rate.observe(rate::showValue)
        viewModel.fee.observe(fee::showValue)
        viewModel.feeCaption.observe { caption.text = it }
        viewModel.continueButtonState.observe(continueButton::setState)
        viewModel.error.observe {
            error.text = it
            error.isVisible = it != null
            retry.isVisible = it != null
        }
    }
}
