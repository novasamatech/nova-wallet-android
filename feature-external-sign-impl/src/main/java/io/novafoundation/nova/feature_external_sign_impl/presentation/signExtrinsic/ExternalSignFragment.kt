package io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic

import androidx.core.os.bundleOf

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.blockBackPressing
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.setProgressState
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
import io.novafoundation.nova.feature_external_sign_impl.databinding.FragmentConfirmSignExtrinsicBinding
import io.novafoundation.nova.feature_external_sign_impl.di.ExternalSignFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class ExternalSignFragment : BaseFragment<ExternalSignViewModel, FragmentConfirmSignExtrinsicBinding>() {

    companion object {

        fun getBundle(payload: ExternalSignPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun createBinding() = FragmentConfirmSignExtrinsicBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.confirmSignExtinsicToolbar.applyStatusBarInsets()

        blockBackPressing()

        binder.confirmDAppActionAllow.prepareForProgress(viewLifecycleOwner)

        binder.confirmDAppActionAllow.setOnClickListener { viewModel.acceptClicked() }
        binder.confirmDAppActionAllow.setText(R.string.common_confirm)
        binder.confirmDAppActionReject.setOnClickListener { viewModel.rejectClicked() }

        binder.confirmSignExtinsicDetails.setOnClickListener { viewModel.detailsClicked() }
        binder.confirmSignExtinsicDetails.background = with(requireContext()) {
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
    override fun subscribe(viewModel: ExternalSignViewModel) {
        setupFeeLoading(viewModel, binder.confirmSignExtinsicFee)
        observeValidations(viewModel)

        viewModel.maybeChainUi.observe { chainUi ->
            binder.confirmSignExtinsicNetwork.postToSelf {
                if (chainUi != null) {
                    showChain(chainUi)
                } else {
                    makeGone()
                }
            }
        }

        viewModel.requestedAccountModel.observe {
            binder.confirmSignExtinsicAccount.postToSelf { showAddress(it) }
        }

        viewModel.walletUi.observe {
            binder.confirmSignExtinsicWallet.postToSelf { showWallet(it) }
        }

        binder.confirmSignExtinsicIcon.showDAppIcon(viewModel.dAppInfo?.icon, imageLoader)
        binder.confirmSignExtinsicDappUrl.showValueOrHide(viewModel.dAppInfo?.url)

        viewModel.performingOperationInProgress.observe { operationInProgress ->
            val actionsAllowed = !operationInProgress

            binder.confirmDAppActionReject.isEnabled = actionsAllowed
            binder.confirmDAppActionAllow.setProgressState(show = operationInProgress)
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

    private fun setupFeeLoading(viewModel: ExternalSignViewModel, feeView: FeeView) {
        val mixin = viewModel.originFeeMixin
        feeView.setVisible(mixin != null)

        mixin?.let { setupFeeLoading(it, feeView) }
    }
}
