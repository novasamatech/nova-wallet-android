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
import kotlinx.android.synthetic.main.fragment_import_parity_signer_scan.scanImportParitySignerScan
import kotlinx.android.synthetic.main.fragment_import_parity_signer_scan.scanImportParitySignerScanToolbar

class ScanImportParitySignerFragment : ScanQrFragment<ScanImportParitySignerViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_parity_signer_scan, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanImportParitySignerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        scanImportParitySignerScanToolbar.applyStatusBarInsets()
        scanImportParitySignerScanToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override val scanView: ScanView
        get() = scanImportParitySignerScan
}
