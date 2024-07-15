package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerFragment

class SelectAddressImportLedgerFragment : SelectAddressLedgerFragment<SelectAddressImportLedgerViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectAddressImportLedgerComponentFactory()
            .create(this, payload())
            .inject(this)
    }
}
