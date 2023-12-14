package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressChain
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressContent
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressToolbar
import javax.inject.Inject

abstract class SelectAddressLedgerFragment<V : SelectAddressLedgerViewModel> :
    BaseFragment<V>(),
    AccountsAdapter.AccountItemHandler,
    LedgerSelectAddressLoadMoreAdapter.Handler {

    companion object {

        private const val PAYLOAD_KEY = "SelectAddressImportLedgerFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerAddressPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val addressesAdapter by lazy(LazyThreadSafetyMode.NONE) { AccountsAdapter(this, imageLoader, AccountsAdapter.Mode.VIEW) }
    private val loadMoreAdapter = LedgerSelectAddressLoadMoreAdapter(handler = this, lifecycleOwner = this)

    @Inject
    lateinit var ledgerMessagePresentable: LedgerMessagePresentable

    protected fun payload(): SelectLedgerAddressPayload = argument(PAYLOAD_KEY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_ledger_select_address, container, false)
    }

    override fun initViews() {
        ledgerSelectAddressToolbar.applyStatusBarInsets()
        ledgerSelectAddressToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        onBackPressed { viewModel.backClicked() }

        ledgerSelectAddressContent.setHasFixedSize(true)
        ledgerSelectAddressContent.adapter = ConcatAdapter(addressesAdapter, loadMoreAdapter)
    }

    override fun subscribe(viewModel: V) {
        viewModel.loadMoreState.observe(loadMoreAdapter::setState)
        viewModel.loadedAccountModels.observe(addressesAdapter::submitList)

        viewModel.chainUi.observe(ledgerSelectAddressChain::setChain)

        setupLedgerMessages(ledgerMessagePresentable)
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun loadMoreClicked() {
        viewModel.loadMoreClicked()
    }
}
