package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import coil.load
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability

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
            .bittensorSubnetAmountComponentFactory().create(this, payload).inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        toolbar.setRightActionClickListener {
            showInfo(R.string.swap_slippage_title, R.string.bittensor_slippage_fixed)
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
        rate.title.setOnClickListener { showInfo(R.string.bittensor_exchange_rate, R.string.swap_rate_description) }
        continueButton.prepareForProgress(viewLifecycleOwner)
        continueButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun subscribe(viewModel: BittensorSubnetAmountViewModel) = with(binder) {
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
                "\u200e" + listOf(name, it.symbol).filter(String::isNotBlank).joinToString(" ")
            } ?: "—"
            subnetIcon.load(it?.logoUrl, FeatureUtils.getCommonApi(requireContext()).imageLoader()) {
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_nova)
                fallback(R.drawable.ic_nova)
                error(R.drawable.ic_nova)
            }
        }
        viewModel.alphaAmount.observe(receive::showValue)
        viewModel.rate.observe(rate::showValue)
        viewModel.continueButtonState.observe(continueButton::setState)
    }

    private fun showInfo(title: Int, message: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.common_ok, null)
            .show()
    }
}
