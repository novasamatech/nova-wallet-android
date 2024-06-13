package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment

class SelectLedgerImportFragment : SelectLedgerFragment<SelectLedgerImportViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectLedgerImportComponentFactory()
            .create(this, payload())
            .inject(this)
    }
}
