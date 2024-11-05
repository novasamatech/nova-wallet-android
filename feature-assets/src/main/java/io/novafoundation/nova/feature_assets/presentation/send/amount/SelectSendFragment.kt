package io.novafoundation.nova.feature_assets.presentation.send.amount

import androidx.core.os.bundleOf

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.addInputKeyboardCallback
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.removeInputKeyboardCallback
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupExternalAccounts
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.setupYourWalletsBtn
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentSelectSendBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.SelectCrossChainDestinationBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

private const val KEY_ADDRESS = "KEY_ADDRESS"
private const val KEY_ASSET_PAYLOAD = "KEY_ASSET_PAYLOAD"

class SelectSendFragment : BaseFragment<SelectSendViewModel, FragmentSelectSendBinding>() {

    companion object {

        fun getBundle(payload: SendPayload, recipientAddress: String? = null) = bundleOf(
            KEY_ADDRESS to recipientAddress,
            KEY_ASSET_PAYLOAD to payload
        )
    }

    override fun createBinding() = FragmentSelectSendBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.chooseAmountContainer.applyStatusBarInsets(false)
        binder.selectSendNext.prepareForProgress(viewLifecycleOwner)
        binder.selectSendNext.setOnClickListener { viewModel.nextClicked() }

        binder.selectSendToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.selectSendOriginChain.setOnClickListener { viewModel.originChainClicked() }
        binder.selectSendDestinationChain.setOnClickListener { viewModel.destinationChainClicked() }

        binder.selectWallet.setOnClickListener { viewModel.selectRecipientWallet() }

        binder.selectSendCrossChainFee.makeGone() // gone inititally
        binder.selectSendCrossChainFee.setTitle(R.string.wallet_send_cross_chain_fee)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .chooseAmountComponentFactory()
            .create(this, argument(KEY_ADDRESS), argument(KEY_ASSET_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectSendViewModel) {
        setupExternalActions(viewModel)

        observeValidations(viewModel)

        setupFeeLoading(viewModel.originFeeMixin, binder.selectSendOriginFee)
        setupFeeLoading(viewModel.crossChainFeeMixin, binder.selectSendCrossChainFee)

        setupAmountChooser(viewModel.amountChooserMixin, binder.selectSendAmount)
        setupAddressInput(viewModel.addressInputMixin, binder.selectSendRecipient)
        setupExternalAccounts(viewModel.addressInputMixin, binder.selectSendRecipient)
        setupYourWalletsBtn(binder.selectWallet, viewModel.selectAddressMixin)

        viewModel.chooseDestinationChain.awaitableActionLiveData.observeEvent {
            removeInputKeyboardCallback(binder.selectSendRecipient)
            val crossChainDestinationBottomSheet = SelectCrossChainDestinationBottomSheet(
                context = requireContext(),
                payload = it.payload,
                onSelected = { _, item -> it.onSuccess(item) },
                onCancelled = it.onCancel
            )
            crossChainDestinationBottomSheet.setOnDismissListener { addInputKeyboardCallback(viewModel.addressInputMixin, binder.selectSendRecipient) }
            crossChainDestinationBottomSheet.show()
        }

        viewModel.transferDirectionModel.observe {
            binder.selectSendOriginChain.setModel(it.originChip)
            binder.selectSendFromTitle.text = it.originChainLabel

            if (it.destinationChip != null) {
                binder.selectSendDestinationChain.setModel(it.destinationChip)
                binder.selectSendDestinationChain.makeVisible()
                binder.selectSendToTitle.makeVisible()
            } else {
                binder.selectSendToTitle.makeGone()
                binder.selectSendDestinationChain.makeGone()
            }
        }

        viewModel.continueButtonStateLiveData.observe(binder.selectSendNext::setState)
    }
}
