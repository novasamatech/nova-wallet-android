package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_start_import_parity_signer.startImportParitySignerScanQrCode
import kotlinx.android.synthetic.main.fragment_start_import_parity_signer.startImportParitySignerToolbar

class StartImportParitySignerFragment : BaseFragment<StartImportParitySignerViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_start_import_parity_signer, container, false)
    }

    override fun initViews() {
        startImportParitySignerToolbar.setHomeButtonListener { viewModel.backClicked() }
        startImportParitySignerToolbar.applyStatusBarInsets()

        startImportParitySignerScanQrCode.setOnClickListener { viewModel.scanQrCodeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .startImportParitySignerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportParitySignerViewModel) {}
}
