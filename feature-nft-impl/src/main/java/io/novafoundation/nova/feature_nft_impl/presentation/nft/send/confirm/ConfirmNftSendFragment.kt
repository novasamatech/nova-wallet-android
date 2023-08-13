package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendConfirm
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendContainer
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendFromNetwork
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendOriginFee
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendRecipient
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendSender
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendToolbar
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.confirmSendWallet
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.nftCollectionName
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.nftName

private const val KEY_DRAFT = "KEY_DRAFT"

class ConfirmNftSendFragment : BaseFragment<ConfirmNftSendViewModel>() {

    companion object {

        fun getBundle(transferDraft: NftTransferDraft): Bundle {
            return bundleOf(KEY_DRAFT to transferDraft)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_confirm_nft_send, container, false)

    override fun initViews() {
        confirmSendContainer.applyStatusBarInsets()

        confirmSendSender.setOnClickListener { viewModel.senderAddressClicked() }
        confirmSendRecipient.setOnClickListener { viewModel.recipientAddressClicked() }

        confirmSendToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmSendConfirm.setOnClickListener { viewModel.submitClicked() }
        confirmSendConfirm.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        val transferDraft = argument<NftTransferDraft>(KEY_DRAFT)

        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .confirmSendComponentFactory()
            .create(this, transferDraft)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmNftSendViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        setupFeeLoading(viewModel.originFeeMixin, confirmSendOriginFee)

        nftName.text = viewModel.transferDraft.name
        nftCollectionName.text = viewModel.transferDraft.collectionName

        viewModel.recipientModel.observe(confirmSendRecipient::showAddress)
        viewModel.senderModel.observe(confirmSendSender::showAddress)

        viewModel.sendButtonStateLiveData.observe(confirmSendConfirm::setState)

        viewModel.wallet.observe(confirmSendWallet::showWallet)

        viewModel.originChainUi.observe {
            confirmSendFromNetwork.showChain(it)
        }
    }
}
