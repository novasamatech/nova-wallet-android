package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.verify.VerifyLedgerAddressBottomSheet
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressChain
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressContent
import kotlinx.android.synthetic.main.fragment_import_ledger_select_address.ledgerSelectAddressToolbar

class SelectAddressImportLedgerFragment : BaseFragment<SelectAddressImportLedgerViewModel>(), AccountsAdapter.AccountItemHandler,
    LedgerSelectAddressLoadMoreAdapter.Handler {

    private var showedVerifyBottomSheet: VerifyLedgerAddressBottomSheet? = null

    companion object {

        private const val PAYLOAD_KEY = "SelectAddressImportLedgerFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerAddressPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    private val addressesAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AccountsAdapter(this, AccountsAdapter.Mode.VIEW)
    }

    private val loadMoreAdapter by lazy(LazyThreadSafetyMode.NONE) {
        LedgerSelectAddressLoadMoreAdapter(handler = this, lifecycleOwner = this)
    }

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

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .selectAddressImportLedgerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectAddressImportLedgerViewModel) {
        observeRetries(viewModel)

        viewModel.loadMoreState.observe(loadMoreAdapter::setState)
        viewModel.loadedAccountModels.observe(addressesAdapter::submitList)

        viewModel.chainUi.observe(ledgerSelectAddressChain::setChain)

        viewModel.verifyAddressCommandEvent.observeEvent {
            when(it){
                VerifyCommand.Hide -> {
                    showedVerifyBottomSheet?.dismiss()
                    showedVerifyBottomSheet = null
                }
                is VerifyCommand.Show -> {
                    showedVerifyBottomSheet = VerifyLedgerAddressBottomSheet(requireContext(), it.onCancel)
                    showedVerifyBottomSheet!!.show()
                }
            }
        }
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun loadMoreClicked() {
        viewModel.loadMoreClicked()
    }
}
