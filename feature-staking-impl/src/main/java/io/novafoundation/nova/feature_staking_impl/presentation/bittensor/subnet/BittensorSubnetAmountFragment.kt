package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import coil.load
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showLoadingValue
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorSubnetIcon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

private const val PAYLOAD_KEY = "payload"

class BittensorSubnetAmountFragment : BaseFragment<BittensorSubnetAmountViewModel, FragmentBittensorSubnetAmountBinding>() {

    companion object {

        fun getBundle(payload: BittensorSubnetAmountPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentBittensorSubnetAmountBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argument<BittensorSubnetAmountPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetAmountComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        toolbar.setRightActionClickListener {
            showInfo(R.string.swap_slippage_title, viewModel.slippageDescription)
        }
        onBackPressed { viewModel.back() }
        amount.amountInput.doAfterTextChanged { viewModel.amountChanged(it.toString()) }
        amount.setMaxActionAvailability(
            MaxActionAvailability.Available {
                amount.amountInput.requestFocus()
                viewModel.maxClicked()
            }
        )
        subnet.setOnClickListener { viewModel.chooseSubnetClicked() }
        subnet.isClickable = !viewModel.existingPosition
        subnetArrow.isVisible = !viewModel.existingPosition
        if (viewModel.existingPosition) {
            toolbar.setTitle(getString(R.string.bittensor_buy_more))
            amount.setTitle(getString(R.string.bittensor_you_pay))
        }
        rate.title.setOnClickListener { showInfo(R.string.bittensor_exchange_rate, getString(R.string.swap_rate_description)) }
        continueButton.prepareForProgress(viewLifecycleOwner)
        continueButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun subscribe(viewModel: BittensorSubnetAmountViewModel) = with(binder) {
        observeValidations(viewModel)
        observeRetries(viewModel.feeLoaderMixin)
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.monthlyEstimate.observe(monthlyTokens::showValue)
        viewModel.projectedAveragePrice.observe(projectedAveragePrice::showLoadingValue)
        viewModel.fee.observe(networkFee::setFeeStatus)
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.amountModel.observe(amount::setChooseAmountModel)
        viewModel.fiatAmount.observe(amount::setFiatAmount)
        viewModel.maxAmount.observe(amount::setMaxAmountDisplay)
        viewModel.amount.observe {
            if (amount.amountInput.text.toString() != it) {
                amount.amountInput.setText(it)
                amount.amountInput.setSelection(it.length)
            }
        }
        viewModel.subnet.observe {
            subnetName.text = it?.let {
                val name = it.name.ifBlank { getString(R.string.bittensor_subnet_number, it.id.value) }
                "\u200e" + it.displayName(name)
            } ?: "—"
            subnetIcon.load(
                it?.logoUrl,
                FeatureUtils.getCommonApi(requireContext())
                    .imageLoader()
            ) {
                bittensorSubnetIcon()
            }
        }
        viewModel.validator.observe(validator::showValue)
        viewModel.apy.observe { subnetApy.text = it }
        viewModel.alphaAmount.observe(receive::showValue)
        viewModel.rate.observe(rate::showValue)
        viewModel.continueButtonState.observe(continueButton::setState)
    }

    private fun showInfo(
        title: Int,
        message: String
    ) {
        DescriptionBottomSheet(requireContext(), getString(title), message)
            .show()
    }
}
