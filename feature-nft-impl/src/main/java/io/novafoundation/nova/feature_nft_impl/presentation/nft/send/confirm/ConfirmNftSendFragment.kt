package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.NftTagsAdapter
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
import kotlinx.android.synthetic.main.fragment_confirm_nft_send.nftName
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsMedia
import kotlinx.android.synthetic.main.fragment_nft_details.tagsRecyclerView
import javax.inject.Inject

private const val KEY_DRAFT = "KEY_DRAFT"

class ConfirmNftSendFragment : BaseFragment<ConfirmNftSendViewModel>() {

    companion object {

        fun getBundle(transferDraft: NftTransferDraft): Bundle {
            return bundleOf(KEY_DRAFT to transferDraft)
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        NftTagsAdapter()
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

        tagsRecyclerView.layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP).apply {
            justifyContent = JustifyContent.FLEX_START
        }
        tagsRecyclerView.adapter = adapter
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

        viewModel.nftDetailsFlow.observe { nftDetails ->
            nftDetailsMedia.load(nftDetails.media, imageLoader) {
                transformations(RoundedCornersTransformation(8.dpF(nftDetailsMedia.context)))
                placeholder(R.drawable.nft_media_progress)
                error(R.drawable.nft_media_error)
                fallback(R.drawable.nft_media_error)
            }
        }

        nftName.text = viewModel.transferDraft.name
        adapter.submitList(viewModel.transferDraft.tags.map { it.uppercase() })

        viewModel.recipientModel.observe(confirmSendRecipient::showAddress)
        viewModel.senderModel.observe(confirmSendSender::showAddress)

        viewModel.sendButtonStateFlow.observe(confirmSendConfirm::setProgress)

        viewModel.wallet.observe(confirmSendWallet::showWallet)

        viewModel.originChainUi.observe {
            confirmSendFromNetwork.showChain(it)
        }
    }
}
