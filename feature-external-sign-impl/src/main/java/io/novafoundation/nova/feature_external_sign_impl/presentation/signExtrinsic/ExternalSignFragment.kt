package io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.blockBackPressing
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import io.novafoundation.nova.feature_external_sign_impl.R
import io.novafoundation.nova.feature_external_sign_impl.di.ExternalSignFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmDAppActionAllow
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmDAppActionReject
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicAccount
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicDappUrl
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicDetails
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicFee
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicIcon
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicNetwork
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicToolbar
import kotlinx.android.synthetic.main.fragment_confirm_sign_extrinsic.confirmSignExtinsicWallet
import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class ExternalSignFragment : BaseFragment<ExternaSignViewModel>() {

    companion object {

        fun getBundle(payload: ExternalSignPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_confirm_sign_extrinsic, container, false)
    }

    override fun initViews() {
        confirmSignExtinsicToolbar.applyStatusBarInsets()

        blockBackPressing()

        confirmDAppActionAllow.prepareForProgress(viewLifecycleOwner)

        confirmDAppActionAllow.setOnClickListener { viewModel.acceptClicked() }
        confirmDAppActionAllow.setText(R.string.common_confirm)
        confirmDAppActionReject.setOnClickListener { viewModel.rejectClicked() }

        confirmSignExtinsicDetails.setOnClickListener { viewModel.detailsClicked() }
        confirmSignExtinsicDetails.background = with(requireContext()) {
            addRipple(getBlockDrawable())
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<ExternalSignFeatureComponent>(this, ExternalSignFeatureApi::class.java)
            .signExtrinsicComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: ExternaSignViewModel) {
        setupFeeLoading(viewModel, confirmSignExtinsicFee)
        observeValidations(viewModel)

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

        confirmSignExtinsicIcon.showDAppIcon(viewModel.dAppInfo?.icon, imageLoader)
        confirmSignExtinsicDappUrl.showValueOrHide(viewModel.dAppInfo?.url)

        viewModel.performingOperationInProgress.observe { operationInProgress ->
            val actionsAllowed = !operationInProgress

            confirmDAppActionReject.isEnabled = actionsAllowed
            confirmDAppActionAllow.setProgress(show = operationInProgress)
        }

        viewModel.confirmUnrecoverableError.awaitableActionLiveData.observeEvent {
            errorDialog(
                context = requireContext(),
                onConfirm = { it.onSuccess(Unit) }
            ) {
                setMessage(it.payload)
            }
        }
    }
}
