package io.novafoundation.nova.feature_assets.presentation.receive

import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.drawToBitmap
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentReceiveBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.receive.model.QrSharingPayload
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ReceiveFragment : BaseFragment<ReceiveViewModel, FragmentReceiveBinding>() {

    override fun createBinding() = FragmentReceiveBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(assetPayload: AssetPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, assetPayload)
        }
    }

    override fun initViews() {
        binder.receiveToolbar.applyStatusBarInsets()

        binder.receiveCopyButton.setOnClickListener { viewModel.copyAddressClicked() }

        binder.receiveBackButton.setOnClickListener { viewModel.backClicked() }

        binder.receiveShare.setOnClickListener {
            val qrBitmap = binder.receiveQrCode.drawToBitmap()
            viewModel.shareButtonClicked(qrBitmap)
        }

        binder.receiveQrCodeContainer.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        binder.receiveQrCodeContainer.clipToOutline = true
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
        viewModel.chainFlow.observe(binder.receiveChain::setChain)
        viewModel.titleFlow.observe(binder.receiveTitle::setText)
        viewModel.subtitleFlow.observe(binder.receiveSubtitle::setText)
        viewModel.qrCodeFlow.observe(binder.receiveQrCode::setQrModel)
        viewModel.accountNameFlow.observe(binder.receiveAccount::setText)
        viewModel.addressFlow.observe(binder.receiveAddress::setText)

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
