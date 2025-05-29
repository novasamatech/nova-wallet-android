package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerFragment

class AddEvmGenericLedgerAccountSelectAddressFragment : SelectAddressLedgerFragment<AddEvmGenericLedgerAccountSelectAddressViewModel>() {

    companion object {
        private const val PAYLOAD_KEY = "AddEvmGenericLedgerAccountSelectAddressFragment.Payload"

        fun getBundle(payload: AddEvmGenericLedgerAccountSelectAddressPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun initViews() {
        super.initViews()

        binder.ledgerSelectAddressChain.makeGone()
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .addEvmGenericLedgerAccountSelectAddressComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
