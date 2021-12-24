package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import coil.request.ImageRequest
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.showDAppIcon
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmInnerContent
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicAccount
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicFee
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicNetwork
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicWallet
import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class DAppSignExtrinsicFragment : BaseBottomSheetFragment<DAppSignExtrinsicViewModel>() {

    companion object {

        fun getBundle(payload: DAppSignExtrinsicPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.bottom_sheet_confirm_dapp_action, container, false)
    }

    override fun initViews() {
        confirmInnerContent.inflateChild(R.layout.bottom_sheet_confirm_sign_extrinsic, attachToRoot = true)
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .signExtrinsicComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: DAppSignExtrinsicViewModel) {
        viewModel.chainUi.observe { chainUi ->
            confirmSignExtinsicNetwork.postToSelf { showValue(chainUi.name) }
            loadImage(chainUi.icon)?.let {
                confirmSignExtinsicNetwork.valuePrimary.setDrawableStart(it, paddingInDp = 8, widthInDp = 24)
            }
        }

        viewModel.addressModel.observe {
            confirmSignExtinsicWallet.valuePrimary.setDrawableStart(it.image, paddingInDp = 8)
            confirmSignExtinsicWallet.postToSelf { showValue(it.name!!) }

            confirmSignExtinsicAccount.valuePrimary.setDrawableStart(it.image, paddingInDp = 8)
            confirmSignExtinsicAccount.postToSelf { showValue(it.address) }
        }

        viewModel.dAppInfo.observe {
            confirmSignExtinsicIcon.showDAppIcon(it.metadata?.iconLink, imageLoader)
        }

        setupFeeLoading(viewModel, confirmSignExtinsicFee)
    }

    private suspend fun loadImage(url: String): Drawable? {
        val request = ImageRequest.Builder(requireContext())
            .data(url)
            .build()

        return imageLoader.execute(request).drawable
    }
}
