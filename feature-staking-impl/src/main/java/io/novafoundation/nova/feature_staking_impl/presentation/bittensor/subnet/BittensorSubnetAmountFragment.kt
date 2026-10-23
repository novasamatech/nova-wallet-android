package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setBittensorFeeStatus

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import coil.load
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ChipLabelView
import androidx.core.widget.TextViewCompat
import android.view.ViewGroup
import android.util.TypedValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorSubnetIcon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.bindGetAsset
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
            viewModel.slippageClicked()
        }
        onBackPressed { viewModel.back() }
        amount.amountInput.doAfterTextChanged { viewModel.amountChanged(it.toString()) }
        amount.setMaxActionAvailability(
            MaxActionAvailability.Available {
                amount.amountInput.requestFocus()
                viewModel.maxClicked()
            }
        )
        subnet.setOnClickListener { viewModel.subnetClicked() }
        manualSubnet.setOnClickListener { viewModel.chooseSubnetClicked() }
        manualSubnet.isVisible = viewModel.isQuickPick
        selectionError.setActionText(getString(R.string.common_try_again))
        selectionError.setOnLinkClickedListener { viewModel.retrySelection() }
        selectionError.setButtonText(getString(R.string.bittensor_see_all_subnets))
        selectionError.setOnButtonClickedListener { viewModel.chooseSubnetClicked() }
        subnet.isClickable = !viewModel.existingPosition
        subnetArrow.isVisible = !viewModel.existingPosition
        projectedAveragePrice.isVisible = viewModel.existingPosition
        validator.setOnClickListener { viewModel.validatorClicked() }
        validator.isClickable = !viewModel.existingPosition
        validator.setPrimaryValueEndIcon(if (viewModel.existingPosition) null else R.drawable.ic_chevron_right)
        if (viewModel.existingPosition) {
            toolbar.setTitle(getString(R.string.bittensor_buy_more))
            amount.setTitle(getString(R.string.bittensor_you_pay))
        }
        rate.title.setOnClickListener { showInfo(R.string.bittensor_exchange_rate, getString(R.string.swap_rate_description)) }
        continueButton.prepareForProgress(viewLifecycleOwner)
        continueButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun subscribe(viewModel: BittensorSubnetAmountViewModel) = with(binder) {
        viewModel.getAssetOptionsMixin.bindGetAsset(fundingCard.getAsset)
        viewModel.fundingMessage.observe { fundingCard.message.text = it }
        viewModel.reserveWarning.observe(reserveWarning::setMessageOrHide)
        viewModel.needsFunding.observe {
            fundingCard.root.isVisible = it
            amount.isVisible = !it
        }
        observeValidations(viewModel)
        observeRetries(viewModel.feeLoaderMixin)
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.selectionLoading.observe { selectionLoading.isVisible = it }
        viewModel.selectionError.observe(selectionError::setMessageOrHide)
        viewModel.subnetChips.observe { chips ->
            subnetChips.removeAllViews()
            subnetChips.isVisible = chips.isNotEmpty()
            chips.forEach { title ->
                val chip = ChipLabelView(requireContext()).apply {
                    text = title
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_NovaFoundation_Regular_Caption1)
                    isAllCaps = false
                    letterSpacing = 0f
                    TextViewCompat.setLineHeight(this, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics).toInt())
                    setTextColorRes(R.color.text_primary)
                    setPadding(8.dp, 4.dp, 8.dp, 4.dp)
                    minHeight = 0
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                subnetChips.addView(chip)
            }
        }
        viewModel.monthlyEstimate.observe(monthlyTokens::showBittensorLoadingValue)
        projectedAveragePrice.isVisible = viewModel.existingPosition
        if (viewModel.existingPosition) {
            viewModel.averagePriceDescription.observe { description ->
                projectedAveragePrice.setOnClickListener {
                    description?.let { showInfo(R.string.bittensor_exit_avg_buy_price, it) }
                }
            }
            viewModel.projectedAveragePrice.observe { state ->
                projectedAveragePrice.showBittensorLoadingAmount(state.map { it.amount })
                projectedAveragePrice.valuePrimary.setTextColorRes(state.dataOrNull?.colorRes ?: R.color.text_secondary)
            }
        }
        viewModel.fee.observe(networkFee::setBittensorFeeStatus)
        viewModel.amountModel.observe {
            amount.setChooseAmountModel(it)
            fundingCard.title.text = getString(R.string.bittensor_get_asset_title, it.input.tokenSymbol)
            fundingCard.icon.setTokenIcon(it.input.tokenIcon, FeatureUtils.getCommonApi(requireContext()).imageLoader())
        }
        viewModel.fiatAmount.observe(amount::setFiatAmount)
        viewModel.maxAmount.observe(amount::setMaxAmountDisplay)
        viewModel.amount.observe {
            if (amount.amountInput.text.toString() != it) {
                amount.amountInput.setText(it)
                amount.amountInput.setSelection(it.length)
            }
        }
        viewModel.subnet.observe {
            selectedSubnetCard.isVisible = it != null || !viewModel.isQuickPick
            subnetName.text = it?.let {
                "\u200e" + it.displayName(requireContext())
            } ?: "—"
            subnetIcon.load(
                it?.logoUrl,
                FeatureUtils.getCommonApi(requireContext())
                    .imageLoader()
            ) {
                bittensorSubnetIcon()
            }
        }
        viewModel.validator.observe(validator::showBittensorLoadingValue)
        viewModel.apy.observe {
            subnetApy.text = it
            subnetApy.isVisible = it != null
        }
        viewModel.annualYield.observe { subnetApy.setTextColorRes(bittensorYieldColor(it)) }
        viewModel.alphaAmount.observe(receive::showBittensorLoadingValue)
        viewModel.rate.observe(rate::showBittensorLoadingValue)
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
