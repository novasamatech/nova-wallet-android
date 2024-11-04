package io.novafoundation.nova.feature_assets.presentation.receive

import android.content.Intent
import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentReceiveBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.receive.model.QrSharingPayload

import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ReceiveFragment : BaseFragment<ReceiveViewModel, FragmentReceiveBinding>() {

    override val binder by viewBinding(FragmentReceiveBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(assetPayload: AssetPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, assetPayload)
        }
    }

    override fun initViews() {
        binder.receiveFrom.setWholeClickListener { viewModel.recipientClicked() }

        binder.receiveToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.receiveShare.setOnClickListener { viewModel.shareButtonClicked() }

        binder.receiveFrom.primaryIcon.setVisible(true)

        binder.receiveQrCode.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        binder.receiveQrCode.clipToOutline = true // for round corners
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .receiveComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ReceiveViewModel) {
        setupExternalActions(viewModel)

        viewModel.qrBitmapFlow.observe(binder.receiveQrCode::setImageBitmap)

        viewModel.receiver.observe {
            binder.receiveFrom.setTextIcon(it.addressModel.image)
            binder.receiveFrom.primaryIcon.loadTokenIcon(it.chainAssetIcon, imageLoader)
            binder.receiveFrom.setMessage(it.addressModel.address)
            binder.receiveFrom.setLabel(it.chain.name)
        }

        viewModel.toolbarTitle.observe(binder.receiveToolbar::setTitle)

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
