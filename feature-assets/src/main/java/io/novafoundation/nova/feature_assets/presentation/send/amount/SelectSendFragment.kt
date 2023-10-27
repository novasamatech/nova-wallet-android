package io.novafoundation.nova.feature_assets.presentation.send.amount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
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
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.SelectCrossChainDestinationBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_select_send.chooseAmountContainer
import kotlinx.android.synthetic.main.fragment_select_send.selectSendAmount
import kotlinx.android.synthetic.main.fragment_select_send.selectSendCrossChainFee
import kotlinx.android.synthetic.main.fragment_select_send.selectSendDestinationChain
import kotlinx.android.synthetic.main.fragment_select_send.selectSendFromTitle
import kotlinx.android.synthetic.main.fragment_select_send.selectSendNext
import kotlinx.android.synthetic.main.fragment_select_send.selectSendOriginChain
import kotlinx.android.synthetic.main.fragment_select_send.selectSendOriginFee
import kotlinx.android.synthetic.main.fragment_select_send.selectSendRecipient
import kotlinx.android.synthetic.main.fragment_select_send.selectSendToTitle
import kotlinx.android.synthetic.main.fragment_select_send.selectSendToolbar
import kotlinx.android.synthetic.main.fragment_select_send.selectWallet

private const val KEY_ADDRESS = "KEY_ADDRESS"
private const val KEY_ASSET_PAYLOAD = "KEY_ASSET_PAYLOAD"

class SelectSendFragment : BaseFragment<SelectSendViewModel>() {

    companion object {

        fun getBundle(assetPayload: AssetPayload, recipientAddress: String? = null) = bundleOf(
            KEY_ADDRESS to recipientAddress,
            KEY_ASSET_PAYLOAD to assetPayload
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_select_send, container, false)

    override fun initViews() {
        chooseAmountContainer.applyStatusBarInsets(false)
        selectSendNext.prepareForProgress(viewLifecycleOwner)
        selectSendNext.setOnClickListener { viewModel.nextClicked() }

        selectSendToolbar.setHomeButtonListener { viewModel.backClicked() }

        selectSendDestinationChain.setOnClickListener { viewModel.destinationChainClicked() }

        selectWallet.background = getRoundedCornerDrawable(cornerSizeDp = 8).withRippleMask(getRippleMask(cornerSizeDp = 8))
        selectWallet.setOnClickListener { viewModel.selectRecipientWallet() }

        selectSendDestinationChain.setChangeable(true)

        selectSendCrossChainFee.makeGone() // gone inititally
        selectSendCrossChainFee.setTitle(R.string.wallet_send_cross_chain_fee)
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

        setupFeeLoading(viewModel.originFeeMixin, selectSendOriginFee)
        setupFeeLoading(viewModel.crossChainFeeMixin, selectSendCrossChainFee)

        setupAmountChooser(viewModel.amountChooserMixin, selectSendAmount)
        setupAddressInput(viewModel.addressInputMixin, selectSendRecipient)
        setupExternalAccounts(viewModel.addressInputMixin, selectSendRecipient)

        viewModel.chooseDestinationChain.awaitableActionLiveData.observeEvent {
            removeInputKeyboardCallback(selectSendRecipient)
            val crossChainDestinationBottomSheet = SelectCrossChainDestinationBottomSheet(
                context = requireContext(),
                payload = it.payload,
                onSelected = { _, item -> it.onSuccess(item) },
                onCancelled = it.onCancel
            )
            crossChainDestinationBottomSheet.setOnDismissListener { addInputKeyboardCallback(viewModel.addressInputMixin, selectSendRecipient) }
            crossChainDestinationBottomSheet.show()
        }

        viewModel.isSelectAddressAvailable.observe {
            selectWallet.isInvisible = !it
        }

        viewModel.transferDirectionModel.observe {
            selectSendOriginChain.setChain(it.originChainUi)
            selectSendFromTitle.text = it.originChainLabel

            if (it.destinationChainUi != null) {
                selectSendDestinationChain.setChain(it.destinationChainUi)
                selectSendDestinationChain.makeVisible()
                selectSendToTitle.makeVisible()
            } else {
                selectSendToTitle.makeGone()
                selectSendDestinationChain.makeGone()
            }
        }

        viewModel.continueButtonStateLiveData.observe(selectSendNext::setState)
    }
}
