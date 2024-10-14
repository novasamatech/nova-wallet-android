package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.setupQrCodeExpiration
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload

class ScanSignParitySignerFragment : ScanQrFragment<ScanSignParitySignerViewModel, FragmentSignParitySignerScanBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "ScanSignParitySignerFragment.Payload"

        fun getBundle(payload: ScanSignParitySignerPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentSignParitySignerScanBinding::bind)

    override val scanView: ScanView
        get() = binder.signParitySignerScanScanner

    override fun initViews() {
        super.initViews()

        binder.signParitySignerScanToolbar.applyStatusBarInsets()
        binder.signParitySignerScanToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanSignParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ScanSignParitySignerViewModel) {
        super.subscribe(viewModel)

        binder.signParitySignerScanToolbar.setTitle(viewModel.title)
        scanView.setTitle(viewModel.scanLabel)

        setupQrCodeExpiration(
            validityPeriodFlow = viewModel.validityPeriodFlow,
            qrCodeExpiredPresentable = viewModel.qrCodeExpiredPresentable,
            timerView = scanView.subtitle,
            onTimerFinished = viewModel::timerFinished
        )

        viewModel.invalidQrConfirmation.awaitableActionLiveData.observeEvent {
            errorDialog(
                context = requireContext(),
                onConfirm = { it.onSuccess(Unit) },
                confirmTextRes = R.string.common_try_again
            ) {
                setTitle(R.string.common_invalid_qr)
                setMessage(R.string.account_parity_signer_sign_qr_invalid_message)
            }
        }
    }
}
