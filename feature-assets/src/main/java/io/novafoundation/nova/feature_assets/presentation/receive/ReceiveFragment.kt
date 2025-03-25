package io.novafoundation.nova.feature_assets.presentation.receive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.receive.model.QrSharingPayload
import kotlinx.android.synthetic.main.fragment_receive.receiveQrCode
import kotlinx.android.synthetic.main.fragment_receive.receiveShare
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_receive.receiveAccount
import kotlinx.android.synthetic.main.fragment_receive.receiveAddress
import kotlinx.android.synthetic.main.fragment_receive.receiveAddressesButton
import kotlinx.android.synthetic.main.fragment_receive.receiveAddressesWarning
import kotlinx.android.synthetic.main.fragment_receive.receiveBackButton
import kotlinx.android.synthetic.main.fragment_receive.receiveChain
import kotlinx.android.synthetic.main.fragment_receive.receiveCopyButton
import kotlinx.android.synthetic.main.fragment_receive.receiveQrCodeContainer
import kotlinx.android.synthetic.main.fragment_receive.receiveSubtitle
import kotlinx.android.synthetic.main.fragment_receive.receiveTitle
import kotlinx.android.synthetic.main.fragment_receive.receiveToolbar

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ReceiveFragment : BaseFragment<ReceiveViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {

        fun getBundle(assetPayload: AssetPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, assetPayload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_receive, container, false)

    override fun initViews() {
        receiveToolbar.applyStatusBarInsets()

        receiveCopyButton.setOnClickListener { viewModel.copyAddressClicked() }

        receiveBackButton.setOnClickListener { viewModel.backClicked() }

        receiveShare.setOnClickListener {
            val qrBitmap = receiveQrCode.drawToBitmap()
            viewModel.shareButtonClicked(qrBitmap)
        }

        receiveQrCodeContainer.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        receiveQrCodeContainer.clipToOutline = true

        receiveAddressesButton.setOnClickListener { viewModel.chainAddressesClicked() }
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
        viewModel.chainFlow.observe(receiveChain::setChain)
        viewModel.titleFlow.observe(receiveTitle::setText)
        viewModel.subtitleFlow.observe(receiveSubtitle::setText)
        viewModel.qrCodeFlow.observe(receiveQrCode::setQrModel)
        viewModel.accountNameFlow.observe(receiveAccount::setText)
        viewModel.addressFlow.observe(receiveAddress::setText)
        viewModel.chainSupportsLegacyAddressFlow.observe {
            receiveAddressesWarning.isVisible = it
            receiveAddressesButton.isVisible = it
        }

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
