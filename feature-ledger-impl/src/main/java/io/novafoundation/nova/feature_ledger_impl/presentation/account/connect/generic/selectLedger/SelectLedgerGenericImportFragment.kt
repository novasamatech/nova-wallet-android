package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger

import android.os.Bundle
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyImportFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyImportFragment.Companion
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload

class SelectLedgerGenericImportFragment : SelectLedgerFragment<SelectLedgerGenericImportViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectLedgerGenericImportFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerGenericPayload): Bundle = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectLedgerGenericImportComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
