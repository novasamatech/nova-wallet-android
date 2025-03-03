package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger

import android.os.Bundle
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment

class SelectLedgerLegacyImportFragment : SelectLedgerFragment<SelectLedgerLegacyImportViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectLedgerLegacyImportFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerLegacyPayload): Bundle = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectLedgerImportComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
