package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerFragment
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressChain

class SelectAddressImportGenericLedgerFragment : SelectAddressLedgerFragment<SelectAddressImportGenericLedgerViewModel>() {

    override fun initViews() {
        super.initViews()

        ledgerSelectAddressChain.makeGone()
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectAddressImportLedgerGenericComponentFactory()
            .create(this, payload())
            .inject(this)
    }
}