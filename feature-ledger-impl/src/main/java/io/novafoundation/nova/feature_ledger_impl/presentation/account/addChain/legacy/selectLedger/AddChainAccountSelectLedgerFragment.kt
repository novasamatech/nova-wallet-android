package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment

class AddChainAccountSelectLedgerFragment : SelectLedgerFragment<AddChainAccountSelectLedgerViewModel>() {

    companion object {
        private const val KEY_ADD_ACCOUNT_PAYLOAD = "AddChainAccountSelectLedgerFragment.Payload"

        fun getBundle(payload: AddChainAccountSelectLedgerPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .addChainAccountSelectLedgerComponentFactory()
            .create(this, argument(KEY_ADD_ACCOUNT_PAYLOAD))
            .inject(this)
    }
}
