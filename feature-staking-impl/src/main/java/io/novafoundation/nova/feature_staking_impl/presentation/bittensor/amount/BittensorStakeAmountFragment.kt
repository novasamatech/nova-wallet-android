package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setBittensorFeeStatus

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorStakeAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.bindGetAsset
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

class BittensorStakeAmountFragment : BaseFragment<BittensorStakeAmountViewModel, FragmentBittensorStakeAmountBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "payload"

        fun getBundle(payload: BittensorStakeAmountPayload?) = Bundle().apply { putParcelable(PAYLOAD_KEY, payload) }
    }

    override fun createBinding() = FragmentBittensorStakeAmountBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.bittensorStakeAmountContinue.prepareForProgress(viewLifecycleOwner)
        binder.bittensorStakeAmountToolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
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
            .create(this, argumentOrNull<BittensorStakeAmountPayload>(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: BittensorStakeAmountViewModel) {
        setupExternalActions(viewModel)
        viewModel.getAssetOptionsMixin.bindGetAsset(binder.fundingCard.getAsset)
        viewModel.fundingMessage.observe { binder.fundingCard.message.text = it }
        viewModel.needsFunding.observe {
            binder.fundingCard.root.isVisible = it
            binder.bittensorStakeAmount.isVisible = !it
        }
        observeValidations(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        viewModel.amountModel.observe {
            binder.bittensorStakeAmount.setChooseAmountModel(it)
            binder.fundingCard.title.text = getString(R.string.bittensor_get_asset_title, it.input.tokenSymbol)
            binder.fundingCard.icon.setTokenIcon(it.input.tokenIcon, FeatureUtils.getCommonApi(requireContext()).imageLoader())
        }
        viewModel.amount.observe(::showAmount)
        viewModel.fiatAmount.observe(binder.bittensorStakeAmount::setFiatAmount)
        viewModel.addStakeCaption.observe(binder.addStakeCaption::setTextOrHide)
        viewModel.reserveWarning.observe(binder.reserveWarning::setMessageOrHide)
        viewModel.monthlyEstimate.observe(binder.monthlyEstimate::showBittensorLoadingValue)
        viewModel.maxAmount.observe(binder.bittensorStakeAmount::setMaxAmountDisplay)
        viewModel.validator.observe(::showValidator)
        viewModel.apy.observe(binder.bittensorStakeAmountApy::showBittensorLoadingValue)
        viewModel.fee.observe(binder.bittensorStakeAmountFee::setBittensorFeeStatus)
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
}
