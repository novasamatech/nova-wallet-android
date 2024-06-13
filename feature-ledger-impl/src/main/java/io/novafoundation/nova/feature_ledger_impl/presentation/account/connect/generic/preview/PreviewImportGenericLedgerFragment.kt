package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.ChainAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.BaseChainAccountsPreviewFragment
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import javax.inject.Inject

class PreviewImportGenericLedgerFragment : BaseChainAccountsPreviewFragment<PreviewImportGenericLedgerViewModel>(), ChainAccountsAdapter.Handler {

    @Inject
    lateinit var ledgerMessagePresentable: LedgerMessagePresentable

    companion object {

        private const val PAYLOAD_KEY = "PreviewImportGenericLedgerFragment.Payload"

        fun getBundle(payload: PreviewImportGenericLedgerPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(
            requireContext(),
            LedgerFeatureApi::class.java
        )
            .previewImportGenericLedgerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: PreviewImportGenericLedgerViewModel) {
        super.subscribe(viewModel)

        setupLedgerMessages(ledgerMessagePresentable)
    }
}
