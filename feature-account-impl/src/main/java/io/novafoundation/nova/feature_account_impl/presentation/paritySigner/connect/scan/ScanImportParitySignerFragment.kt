package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan

import android.os.Bundle

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentImportParitySignerScanBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload

class ScanImportParitySignerFragment : ScanQrFragment<ScanImportParitySignerViewModel, FragmentImportParitySignerScanBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "ScanImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerStartPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun createBinding() = FragmentImportParitySignerScanBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        binder.scanImportParitySignerScanToolbar.applyStatusBarInsets()
        binder.scanImportParitySignerScanToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.scanImportParitySignerScan.setTitle(viewModel.title)
    }

    override val scanView: ScanView
        get() = binder.scanImportParitySignerScan
}
