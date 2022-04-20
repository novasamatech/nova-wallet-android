package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.showDAppIcon
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmDAppActionAllow
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmDAppActionReject
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmInnerContent
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicAccount
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicDetails
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicFee
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicNetwork
import kotlinx.android.synthetic.main.bottom_sheet_confirm_sign_extrinsic.confirmSignExtinsicWallet
import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class DAppSignExtrinsicFragment : BaseBottomSheetFragment<DAppSignViewModel>() {

    companion object {

        fun getBundle(payload: DAppSignPayload) = bundleOf(PAYLOAD_KEY to payload)
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

        confirmDAppActionAllow.setOnClickListener { viewModel.acceptClicked() }
        confirmDAppActionAllow.setText(R.string.common_confirm)
        confirmDAppActionReject.setOnClickListener { viewModel.rejectClicked() }

        confirmSignExtinsicDetails.setOnClickListener { viewModel.detailsClicked() }
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.cancelled()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .signExtrinsicComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: DAppSignViewModel) {
        viewModel.maybeChainUi.observe { chainUi ->
            confirmSignExtinsicNetwork.postToSelf {
                if (chainUi != null) {
                    showChain(chainUi)
                } else {
                    makeGone()
                }
            }
        }

        viewModel.requestedAccountModel.observe {
            confirmSignExtinsicAccount.postToSelf { showAddress(it) }
        }

        viewModel.walletUi.observe {
            confirmSignExtinsicWallet.postToSelf { showWallet(it) }
        }

        viewModel.dAppInfo.observe {
            confirmSignExtinsicIcon.showDAppIcon(it.metadata?.iconLink, imageLoader)
        }

        setupFeeLoading(viewModel, confirmSignExtinsicFee)
    }
}
