package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.databinding.FragmentWcScanBinding
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent

class WalletConnectScanFragment : ScanQrFragment<WalletConnectScanViewModel, FragmentWcScanBinding>() {

    override fun createBinding() = FragmentWcScanBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(requireContext(), WalletConnectFeatureApi::class.java)
            .walletConnectScanComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        super.initViews()

        binder.walletConnectScanToolbar.applyStatusBarInsets()
        binder.walletConnectScanToolbar.setHomeButtonListener { viewModel.backClicked() }

        scanView.subtitle.setDrawableStart(R.drawable.ic_wallet_connect, widthInDp = 24, paddingInDp = 2, tint = R.color.icon_primary)
        scanView.subtitle.setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline)
        scanView.subtitle.setTextColorRes(R.color.text_primary)
    }

    override val scanView: ScanView
        get() = binder.walletConnectScan
}
