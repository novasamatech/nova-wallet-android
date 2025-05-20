package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.setupAddressActions
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentImportLedgerSelectAddressBinding
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.list.LedgerAccountAdapter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.list.LedgerSelectAddressLoadMoreAdapter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.LedgerAccountModel
import javax.inject.Inject

abstract class SelectAddressLedgerFragment<V : SelectAddressLedgerViewModel> :
    BaseFragment<V, FragmentImportLedgerSelectAddressBinding>(),
    LedgerSelectAddressLoadMoreAdapter.Handler,
    LedgerAccountAdapter.Handler {

    companion object {

        private const val PAYLOAD_KEY = "SelectAddressImportLedgerFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerAddressPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun createBinding() = FragmentImportLedgerSelectAddressBinding.inflate(layoutInflater)

    private val addressesAdapter by lazy(LazyThreadSafetyMode.NONE) {
        LedgerAccountAdapter(this)
    }
    private val loadMoreAdapter = LedgerSelectAddressLoadMoreAdapter(handler = this, lifecycleOwner = this)

    @Inject
    lateinit var ledgerMessagePresentable: LedgerMessagePresentable

    protected fun payload(): SelectLedgerAddressPayload = argument(PAYLOAD_KEY)

    override fun initViews() {
        binder.ledgerSelectAddressToolbar.applyStatusBarInsets()
        binder.ledgerSelectAddressToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        onBackPressed { viewModel.backClicked() }

        binder.ledgerSelectAddressContent.setHasFixedSize(true)
        binder.ledgerSelectAddressContent.adapter = ConcatAdapter(addressesAdapter, loadMoreAdapter)
    }

    override fun subscribe(viewModel: V) {
        viewModel.loadMoreState.observe(loadMoreAdapter::setState)
        viewModel.loadedAccountModels.observe(addressesAdapter::submitList)

        viewModel.chainUi.observe(binder.ledgerSelectAddressChain::setChain)

        viewModel.alertFlow.observe(binder.ledgerSelectAddressAlert::setModelOrHide)

        setupLedgerMessages(ledgerMessagePresentable)

        viewModel.addressActionsMixin.setupAddressActions()
    }

    override fun loadMoreClicked() {
        viewModel.loadMoreClicked()
    }

    override fun itemClicked(item: LedgerAccountModel) {
        viewModel.accountClicked(item)
    }

    override fun addressInfoClicked(addressModel: AddressModel, addressScheme: AddressScheme) {
        viewModel.addressInfoClicked(addressModel, addressScheme)
    }
}
