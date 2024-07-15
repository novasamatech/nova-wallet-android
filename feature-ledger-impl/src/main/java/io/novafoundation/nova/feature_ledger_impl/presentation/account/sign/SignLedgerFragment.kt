package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment

class SignLedgerFragment : SelectLedgerFragment<SignLedgerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "SignLedgerFragment.Payload"

        fun getBundle(
            payload: SignLedgerPayload
        ): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .signLedgerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
