package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameFragment
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent

class FinishImportGenericLedgerFragment : CreateWalletNameFragment<FinishImportGenericLedgerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "FinishImportLedgerFragment.Payload"

        fun getBundle(payload: FinishImportGenericLedgerPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .finishGenericImportLedgerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
