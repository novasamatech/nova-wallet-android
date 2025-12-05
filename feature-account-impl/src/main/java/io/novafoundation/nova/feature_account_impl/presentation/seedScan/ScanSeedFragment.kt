package io.novafoundation.nova.feature_account_impl.presentation.seedScan

import android.view.View
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.scan.ScanQrFragment
import io.novafoundation.nova.common.presentation.scan.ScanView
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentScanSeedBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class ScanSeedFragment : ScanQrFragment<ScanSeedViewModel, FragmentScanSeedBinding>() {

    override val scanView: ScanView
        get() = binder.scanSeedScanView

    override fun createBinding() = FragmentScanSeedBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanSeedComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun applyInsets(rootView: View) {
        binder.scanSeedToolbar.applySystemBarInsets()
    }

    override fun initViews() {
        super.initViews()

        binder.scanSeedToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.scanSeedScanView.setTitle(viewModel.title)
    }
}
