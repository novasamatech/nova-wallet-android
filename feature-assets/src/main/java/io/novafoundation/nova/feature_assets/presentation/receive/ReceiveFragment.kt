package io.novafoundation.nova.feature_assets.presentation.receive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.receive.model.QrSharingPayload
import kotlinx.android.synthetic.main.fragment_receive.receiveFrom
import kotlinx.android.synthetic.main.fragment_receive.receiveQrCode
import kotlinx.android.synthetic.main.fragment_receive.receiveShare
import kotlinx.android.synthetic.main.fragment_receive.receiveToolbar
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"
private const val KEY_CHAIN_ID = "KEY_CHAIN"
private const val KEY_TITLE_RES = "KEY_TITLE_RES"

class ReceiveFragment : BaseFragment<ReceiveViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(payload: ReceivePayload) = Bundle().apply {
            when (payload) {
                is ReceivePayload.Asset -> {
                    putParcelable(KEY_PAYLOAD, payload.assetPayload)
                }
                is ReceivePayload.Chain -> {
                    putString(KEY_CHAIN_ID, payload.chainId)
                    payload.titleRes?.let { putInt(KEY_TITLE_RES, it) }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_receive, container, false)

    override fun initViews() {
        receiveFrom.setWholeClickListener { viewModel.recipientClicked() }

        receiveToolbar.setHomeButtonListener { viewModel.backClicked() }

        receiveShare.setOnClickListener { viewModel.shareButtonClicked() }

        receiveFrom.primaryIcon.setVisible(true)

        receiveQrCode.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        receiveQrCode.clipToOutline = true // for round corners
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .receiveComponentFactory()
            .create(this, argument(KEY_PAYLOAD), argument(KEY_CHAIN_ID))
            .inject(this)
    }

    override fun subscribe(viewModel: ReceiveViewModel) {
        setupExternalActions(viewModel)

        viewModel.qrBitmapFlow.observe(receiveQrCode::setImageBitmap)

        viewModel.receiver.observe {
            receiveFrom.setTextIcon(it.addressModel.image)
            receiveFrom.primaryIcon.loadTokenIcon(it.chainAssetIcon, imageLoader)
            receiveFrom.setMessage(it.addressModel.address)
            receiveFrom.setLabel(it.chain.name)
        }

        argument<Int?>(KEY_TITLE_RES)?.let {
            receiveToolbar.setTitle(it)
        } ?: viewModel.toolbarTitle.observe(receiveToolbar::setTitle)

        viewModel.shareEvent.observeEvent(::startQrSharingIntent)
    }

    private fun startQrSharingIntent(qrSharingPayload: QrSharingPayload) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, qrSharingPayload.fileUri)
            putExtra(Intent.EXTRA_TEXT, qrSharingPayload.shareMessage)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.wallet_receive_description_v2_2_0)))
    }
}
