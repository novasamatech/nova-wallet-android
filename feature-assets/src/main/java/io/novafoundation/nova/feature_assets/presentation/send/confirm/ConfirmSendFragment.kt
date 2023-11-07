package io.novafoundation.nova.feature_assets.presentation.send.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_account_api.view.showChainOrHide
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendAmount
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendConfirm
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendContainer
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendCrossChainFee
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendFromNetwork
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendHints
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendOriginFee
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendRecipient
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendSender
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendToNetwork
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendToolbar
import kotlinx.android.synthetic.main.fragment_confirm_send.confirmSendWallet

private const val KEY_DRAFT = "KEY_DRAFT"

class ConfirmSendFragment : BaseFragment<ConfirmSendViewModel>() {

    companion object {

        fun getBundle(transferDraft: TransferDraft) = Bundle().apply {
            putParcelable(KEY_DRAFT, transferDraft)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_confirm_send, container, false)

    override fun initViews() {
        confirmSendContainer.applyStatusBarInsets()

        confirmSendSender.setOnClickListener { viewModel.senderAddressClicked() }
        confirmSendRecipient.setOnClickListener { viewModel.recipientAddressClicked() }

        confirmSendToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmSendConfirm.setOnClickListener { viewModel.submitClicked() }
        confirmSendConfirm.prepareForProgress(viewLifecycleOwner)

        confirmSendCrossChainFee.setTitle(R.string.wallet_send_cross_chain_fee)
        confirmSendCrossChainFee.setFeeStatus(FeeStatus.NoFee) // hide by default
    }

    override fun inject() {
        val transferDraft = argument<TransferDraft>(KEY_DRAFT)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .confirmTransferComponentFactory()
            .create(this, transferDraft)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmSendViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        setupFeeLoading(viewModel.originFeeMixin, confirmSendOriginFee)
        setupFeeLoading(viewModel.crossChainFeeMixin, confirmSendCrossChainFee)
        observeHints(viewModel.hintsMixin, confirmSendHints)

        viewModel.recipientModel.observe(confirmSendRecipient::showAddress)
        viewModel.senderModel.observe(confirmSendSender::showAddress)

        viewModel.sendButtonStateFlow.observe(confirmSendConfirm::setProgress)

        viewModel.wallet.observe(confirmSendWallet::showWallet)

        viewModel.transferDirectionModel.observe {
            confirmSendFromNetwork.showChain(it.originChainUi)
            confirmSendFromNetwork.setTitle(it.originChainLabel)

            confirmSendToNetwork.showChainOrHide(it.destinationChainUi)
        }

        viewModel.amountModel.observe(confirmSendAmount::setAmount)
    }
}
