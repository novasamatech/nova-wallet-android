package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
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

    override fun networkAppIsInstalledStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_1,
        R.string.account_ledger_import_start_step_1_highlighted
    )

    override fun openingNetworkAppStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_2,
        R.string.account_ledger_import_start_step_2_highlighted
    )
}
