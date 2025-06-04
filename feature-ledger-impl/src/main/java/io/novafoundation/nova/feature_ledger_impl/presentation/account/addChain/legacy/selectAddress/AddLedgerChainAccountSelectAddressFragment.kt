package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerFragment

class AddLedgerChainAccountSelectAddressFragment : SelectAddressLedgerFragment<AddLedgerChainAccountSelectAddressViewModel>() {

    companion object {
        private const val PAYLOAD_KEY = "AddChainAccountSelectAddressLedgerFragment.Payload"

        fun getBundle(payload: AddLedgerChainAccountSelectAddressPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .addChainAccountSelectAddressComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
