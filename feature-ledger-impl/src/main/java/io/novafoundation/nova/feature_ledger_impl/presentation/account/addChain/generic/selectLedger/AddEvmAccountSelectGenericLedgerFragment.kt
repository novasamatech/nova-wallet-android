package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment

class AddEvmAccountSelectGenericLedgerFragment : SelectLedgerFragment<AddEvmAccountSelectGenericLedgerViewModel>() {

    companion object {
        private const val KEY_ADD_ACCOUNT_PAYLOAD = "AddEvmAccountSelectGenericLedgerFragment.Payload"

        fun getBundle(payload: AddEvmAccountSelectGenericLedgerPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .addEvmAccountSelectGenericLedgerComponentFactory()
            .create(this, argument(KEY_ADD_ACCOUNT_PAYLOAD))
            .inject(this)
    }
}
