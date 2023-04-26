package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
import kotlinx.android.synthetic.main.fragment_wc_scan.walletConnectScan
import kotlinx.android.synthetic.main.fragment_wc_scan.walletConnectScanToolbar

class WalletConnectScanFragment : ScanQrFragment<WalletConnectScanViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wc_scan, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(requireContext(), WalletConnectFeatureApi::class.java)
            .walletConnectScanComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        walletConnectScanToolbar.applyStatusBarInsets()
        walletConnectScanToolbar.setHomeButtonListener { viewModel.backClicked() }

        scanView.subtitle.setDrawableStart(R.drawable.ic_wallet_connect, widthInDp = 24, paddingInDp = 2, tint = R.color.icon_primary)
        scanView.subtitle.setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline)
        scanView.subtitle.setTextColorRes(R.color.text_primary)
    }

    override val scanView: ScanView
        get() = walletConnectScan
}
