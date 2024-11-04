package io.novafoundation.nova.feature_assets.presentation.send.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_account_api.view.showChainOrHide
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentConfirmSendBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

private const val KEY_DRAFT = "KEY_DRAFT"

class ConfirmSendFragment : BaseFragment<ConfirmSendViewModel, FragmentConfirmSendBinding>() {

    companion object {

        fun getBundle(transferDraft: TransferDraft) = Bundle().apply {
            putParcelable(KEY_DRAFT, transferDraft)
        }
    }

    override fun createBinding() = FragmentConfirmSendBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmSendContainer.applyStatusBarInsets()

        binder.confirmSendSender.setOnClickListener { viewModel.senderAddressClicked() }
        binder.confirmSendRecipient.setOnClickListener { viewModel.recipientAddressClicked() }

        binder.confirmSendToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.confirmSendConfirm.setOnClickListener { viewModel.submitClicked() }
        binder.confirmSendConfirm.prepareForProgress(viewLifecycleOwner)

        binder.confirmSendCrossChainFee.setTitle(R.string.wallet_send_cross_chain_fee)
        binder.confirmSendCrossChainFee.setFeeStatus(FeeStatus.NoFee) // hide by default
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
        setupFeeLoading(viewModel.originFeeMixin, binder.confirmSendOriginFee)
        setupFeeLoading(viewModel.crossChainFeeMixin, binder.confirmSendCrossChainFee)
        observeHints(viewModel.hintsMixin, binder.confirmSendHints)

        viewModel.recipientModel.observe(binder.confirmSendRecipient::showAddress)
        viewModel.senderModel.observe(binder.confirmSendSender::showAddress)

        viewModel.sendButtonStateLiveData.observe(binder.confirmSendConfirm::setState)

        viewModel.wallet.observe(binder.confirmSendWallet::showWallet)

        viewModel.transferDirectionModel.observe {
            binder.confirmSendFromNetwork.showChain(it.origin)
            binder.confirmSendFromNetwork.setTitle(it.originChainLabel)

            binder.confirmSendToNetwork.showChainOrHide(it.destination)
        }

        viewModel.amountModel.observe(binder.confirmSendAmount::setAmount)
    }
}
