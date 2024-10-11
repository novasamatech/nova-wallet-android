package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload

class ScanImportParitySignerFragment : ScanQrFragment<ScanImportParitySignerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "ScanImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerStartPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_parity_signer_scan, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        scanImportParitySignerScanToolbar.applyStatusBarInsets()
        scanImportParitySignerScanToolbar.setHomeButtonListener { viewModel.backClicked() }

        scanImportParitySignerScan.setTitle(viewModel.title)
    }

    override val scanView: ScanView
        get() = scanImportParitySignerScan
}
