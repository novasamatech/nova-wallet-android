package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentImportLedgerFillWalletBinding
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.model.FillableChainAccountModel

import javax.inject.Inject

class FillWalletImportLedgerFragment :
    BaseFragment<FillWalletImportLedgerViewModel, FragmentImportLedgerFillWalletBinding>(),
    FillWalletImportLedgerAdapter.Handler {

    override fun createBinding() = FragmentImportLedgerFillWalletBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        FillWalletImportLedgerAdapter(this, imageLoader)
    }

    override fun initViews() {
        binder.fillWalletImportLedgerToolbar.applyStatusBarInsets()
        binder.fillWalletImportLedgerToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
        onBackPressed { viewModel.backClicked() }

        binder.fillWalletImportLedgerAccounts.setHasFixedSize(true)
        binder.fillWalletImportLedgerAccounts.adapter = adapter

        binder.fillWalletImportLedgerContinue.setOnClickListener { viewModel.continueClicked() }
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
        viewModel.continueState.observe(binder.fillWalletImportLedgerContinue::setState)
        viewModel.fillableChainAccountModels.observe(adapter::submitList)

        viewModel.confirmExit.awaitableActionLiveData.observeEvent {
            warningDialog(
                context = requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                negativeTextRes = R.string.common_no,
                positiveTextRes = R.string.common_yes,
            ) {
                setTitle(R.string.common_cancel_operation_warning)
            }
        }
    }
}
