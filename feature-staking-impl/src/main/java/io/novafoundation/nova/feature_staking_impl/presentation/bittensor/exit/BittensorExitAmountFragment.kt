package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.view.setMessageOrHide

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorExitAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
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
        val payload = argument<BittensorExitPositionPayload>(PAYLOAD)
        if (payload.netuid().isRoot()) {
            toolbar.hideRightAction()
        } else {
            toolbar.setRightActionClickListener { viewModel.openSlippageSettings() }
        }
        onBackPressed { viewModel.back() }
        amount.setTitle(getString(if (viewModel.isRoot) R.string.bittensor_exit_you_unstake else R.string.bittensor_exit_you_sell))
        amount.setAssetIconRounded(!viewModel.isRoot)
        amount.amountInput.doAfterTextChanged { viewModel.amountChanged(it.toString()) }
        amount.setMaxActionAvailability(MaxActionAvailability.Available { viewModel.maxClicked() })
        receive.isVisible = !viewModel.isRoot
        rate.isVisible = !viewModel.isRoot
        averagePrice.isVisible = !viewModel.isRoot
        earned.isVisible = !viewModel.isRoot
        validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        validator.setOnClickListener { viewModel.validatorClicked() }
        continueButton.prepareForProgress(viewLifecycleOwner)
        continueButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun subscribe(viewModel: BittensorExitAmountViewModel) = with(binder) {
        setupExternalActions(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        observeValidations(viewModel)
        viewModel.title.observe(toolbar::setTitle)
        viewModel.amountModel.observe { it?.let(amount::setChooseAmountModel) }
        viewModel.amount.observe { if (amount.amountInput.text.toString() != it) amount.amountInput.setText(it) }
        viewModel.maxAmount.observe(amount::setMaxAmountDisplay)
        viewModel.fiatAmount.observe(amount::setFiatAmount)
        viewModel.amountError.observe(amount::setError)
        viewModel.validator.observe { validator.showValue(it) }
        if (!viewModel.isRoot) {
            viewModel.averagePrice.observe(averagePrice::showBittensorLoadingAmount)
            viewModel.averagePriceDescription.observe { description ->
                averagePrice.setOnClickListener {
                    description?.let { DescriptionBottomSheet(requireContext(), getString(R.string.bittensor_exit_avg_buy_price), it).show() }
                }
            }
            viewModel.earned.observe { state ->
                earned.showBittensorLoadingAmount(state.map { it.amount })
                earned.valuePrimary.setTextColorRes(state.dataOrNull?.colorRes ?: R.color.text_secondary)
            }
            viewModel.received.observe(receive::showBittensorLoadingValue)
            viewModel.rate.observe(rate::showBittensorLoadingValue)
            viewModel.swapRateDescription.observe { description ->
                rate.setOnClickListener {
                    description?.let { DescriptionBottomSheet(requireContext(), getString(R.string.bittensor_exchange_rate), it).show() }
                }
            }
        }
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
        }
        viewModel.fee.observe(fee::showBittensorLoadingValue)
        viewModel.feeCaption.observe {
            caption.text = it
            caption.isVisible = viewModel.isRoot && it != null
        }
        viewModel.saleFeeWarning.observe(saleFeeWarning::setMessageOrHide)
        viewModel.continueButtonState.observe(continueButton::setState)
    }
}
