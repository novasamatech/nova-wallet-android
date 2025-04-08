package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start.StartImportLedgerFragment

class StartImportGenericLedgerFragment : StartImportLedgerFragment<StartImportGenericLedgerViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .startImportGenericLedgerComponentFactory()
            .create(this)
            .inject(this)
    }
}
