package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.model.FillableChainAccountModel
import kotlinx.android.synthetic.main.fragment_import_ledger_fill_wallet.fillWalletImportLedgerAccounts
import kotlinx.android.synthetic.main.fragment_import_ledger_fill_wallet.fillWalletImportLedgerContinue
import kotlinx.android.synthetic.main.fragment_import_ledger_fill_wallet.fillWalletImportLedgerToolbar
import javax.inject.Inject

class FillWalletImportLedgerFragment : BaseFragment<FillWalletImportLedgerViewModel>(), FillWalletImportLedgerAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        FillWalletImportLedgerAdapter(this, imageLoader)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_ledger_fill_wallet, container, false)
    }

    override fun initViews() {
        fillWalletImportLedgerToolbar.applyStatusBarInsets()
        fillWalletImportLedgerToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        onBackPressed { viewModel.backClicked() }

        fillWalletImportLedgerAccounts.setHasFixedSize(true)
        fillWalletImportLedgerAccounts.adapter = adapter

        fillWalletImportLedgerContinue.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .fillWalletImportLedgerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun onItemClicked(item: FillableChainAccountModel) {
        viewModel.itemClicked(item)
    }

    override fun subscribe(viewModel: FillWalletImportLedgerViewModel) {
        viewModel.continueState.observe(fillWalletImportLedgerContinue::setState)
        viewModel.fillableChainAccountModels.observe(adapter::submitList)

        viewModel.confirmExit.awaitableActionLiveData.observeEvent {
            warningDialog(
                context = requireContext(),
                onConfirm = { it.onSuccess(true) },
                onCancel = { it.onSuccess(false) },
                cancelTextRes = R.string.common_no,
                confirmTextRes = R.string.common_yes,
            ) {
                setTitle(R.string.common_cancel_operation_warning)
            }
        }
    }
}
