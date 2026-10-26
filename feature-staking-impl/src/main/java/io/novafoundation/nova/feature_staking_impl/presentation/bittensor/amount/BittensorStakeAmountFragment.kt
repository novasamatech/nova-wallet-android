package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setBittensorFeeStatus

import android.os.Bundle
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectionState
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bindBittensorGetAsset
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setSelectable
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showValidator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

class BittensorStakeAmountFragment : BaseFragment<BittensorStakeAmountViewModel, FragmentBittensorStakeAmountBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "payload"

        private const val SELECTION_STATE_KEY = "selectionState"

        fun getBundle(
            payload: BittensorStakeAmountPayload?,
            selectionState: BittensorSelectionState = BittensorSelectionState()
        ) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
            putParcelable(SELECTION_STATE_KEY, selectionState)
        }
    }

    override fun createBinding() = FragmentBittensorStakeAmountBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.bittensorStakeAmountContinue.prepareForProgress(viewLifecycleOwner)
        binder.bittensorStakeAmountToolbar.setTitle(viewModel.titleRes)
        binder.bittensorStakeAmountToolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        showEntry(viewModel.entry)
        binder.bittensorStakeAmountValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.quickPickValidator.root.setOnClickListener { viewModel.validatorClicked() }
        binder.quickPickValidator.setSelectable(true)
        binder.pickerValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.quickPickHeader.setOnClickListener { viewModel.quickPickHeaderClicked() }
        binder.quickPickSeeAll.setOnClickListener { viewModel.seeAllSubnetsClicked() }
        binder.pickerSeeAll.setOnClickListener { viewModel.seeAllSubnetsClicked() }
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
            .create(
                this,
                argumentOrNull<BittensorStakeAmountPayload>(PAYLOAD_KEY),
                argumentOrNull<BittensorSelectionState>(SELECTION_STATE_KEY) ?: BittensorSelectionState()
            )
            .inject(this)
    }

    override fun subscribe(viewModel: BittensorStakeAmountViewModel) {
        setupExternalActions(viewModel)
        viewModel.getAssetOptionsMixin.bindBittensorGetAsset(binder.fundingCard.getAsset, viewModel.getAssetLabel)
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
            binder.fundingCard.icon.setTokenIcon(it.input.tokenIcon, imageLoader)
            binder.quickPickIcon.setTokenIcon(it.input.tokenIcon, imageLoader)
        }
        viewModel.amount.observe(::showAmount)
        viewModel.fiatAmount.observe(binder.bittensorStakeAmount::setFiatAmount)
        viewModel.addStakeCaption.observe(binder.addStakeCaption::setTextOrHide)
        viewModel.reserveWarning.observe(binder.reserveWarning::setMessageOrHide)
        viewModel.maxAmount.observe(binder.bittensorStakeAmount::setMaxAmountDisplay)
        viewModel.validator.observe(::showValidator)
        viewModel.apy.observe(binder.bittensorStakeAmountApy::showBittensorLoadingValue)
        viewModel.quickPickValidator.observe { binder.quickPickValidator.showValidator(it, imageLoader) }
        viewModel.bareApy.observe(::showPickerRate)
        viewModel.apyColorRes.observe(binder.pickerValidatorRate::setTextColorRes)
        viewModel.fee.observe {
            binder.bittensorStakeAmountFee.setBittensorFeeStatus(it)
            binder.quickPickFee.setBittensorFeeStatus(it)
            binder.pickerFee.setBittensorFeeStatus(it)
        }
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

    private val imageLoader by lazy(LazyThreadSafetyMode.NONE) { FeatureUtils.getCommonApi(requireContext()).imageLoader() }

    private fun showEntry(entry: BittensorRootStakeEntry) = with(binder) {
        quickPickSection.isVisible = entry == BittensorRootStakeEntry.QUICK_PICK
        quickPickCard.isVisible = entry == BittensorRootStakeEntry.QUICK_PICK
        pickerContainer.isVisible = entry == BittensorRootStakeEntry.PICKER
        pickerSeeAll.isVisible = entry == BittensorRootStakeEntry.PICKER
        addStakeContainer.isVisible = entry == BittensorRootStakeEntry.ADD_STAKE
        if (entry == BittensorRootStakeEntry.ADD_STAKE) bittensorStakeAmountToolbar.setHomeButtonIcon(R.drawable.ic_close)
    }

    private fun showValidator(validator: BittensorValidatorUi) = with(binder) {
        bittensorStakeAmountValidator.showValue(validator.name)
        validator.icon?.let { bittensorStakeAmountValidator.loadImage(it) }
            ?: bittensorStakeAmountValidator.setImage(R.drawable.ic_nova, 20)
        pickerValidatorName.text = validator.name
        validator.icon?.let { pickerValidatorIcon.setIcon(it, imageLoader) } ?: pickerValidatorIcon.setImageResource(R.drawable.ic_nova)
    }

    private fun showPickerRate(state: ExtendedLoadingState<String>) = with(binder) {
        pickerValidatorRateProgress.isVisible = state is ExtendedLoadingState.Loading
        pickerValidatorRate.isVisible = state !is ExtendedLoadingState.Loading
        pickerValidatorRate.text = state.dataOrNull ?: "—"
    }
}
