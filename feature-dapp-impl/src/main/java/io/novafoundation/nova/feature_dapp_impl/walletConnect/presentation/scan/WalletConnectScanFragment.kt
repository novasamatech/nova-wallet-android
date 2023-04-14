package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import kotlinx.android.synthetic.main.fragment_wc_scan.walletConnectScan
import kotlinx.android.synthetic.main.fragment_wc_scan.walletConnectScanToolbar

class WalletConnectScanFragment : ScanQrFragment<WalletConnectScanViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wc_scan, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(requireContext(), DAppFeatureApi::class.java)
            .walletConnectScanComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        walletConnectScanToolbar.applyStatusBarInsets()
        walletConnectScanToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override val scanView: ScanView
        get() = walletConnectScan
}
