package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start.StartImportLedgerFragment

class StartImportLegacyLedgerFragment : StartImportLedgerFragment<StartImportLegacyLedgerViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .startImportLegacyLedgerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportLegacyLedgerViewModel) {
        super.subscribe(viewModel)

        viewModel.warningModel.observe {
            pageAdapter.showWarning(it)
        }
    }
}
