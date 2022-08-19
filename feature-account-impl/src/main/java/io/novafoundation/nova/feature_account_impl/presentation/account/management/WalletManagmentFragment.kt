package io.novafoundation.nova.feature_account_impl.presentation.account.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.android.synthetic.main.fragment_accounts.accountListToolbar
import kotlinx.android.synthetic.main.fragment_accounts.accountsList
import kotlinx.android.synthetic.main.fragment_accounts.addAccount

class WalletManagmentFragment : BaseFragment<WalletManagmentViewModel>(), AccountsAdapter.AccountItemHandler {
    private lateinit var adapter: AccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = layoutInflater.inflate(R.layout.fragment_accounts, container, false)

    override fun initViews() {
        adapter = AccountsAdapter(this, initialMode = viewModel.mode.value)

        accountsList.setHasFixedSize(true)
        accountsList.adapter = adapter

        accountListToolbar.setRightActionClickListener { viewModel.editClicked() }
        accountListToolbar.setHomeButtonListener { viewModel.backClicked() }

        addAccount.setOnClickListener { viewModel.addAccountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .walletManagmentComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WalletManagmentViewModel) {
        viewModel.walletsListingMixin.metaAccountsFlow.observe(adapter::submitList)
        viewModel.mode.observe(adapter::setMode)

        viewModel.toolbarAction.observe(accountListToolbar::setTextRight)

        viewModel.confirmAccountDeletion.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onConfirm = { it.onSuccess(true) },
                onCancel = { it.onSuccess(false) },
                confirmTextRes = R.string.account_delete_confirm
            ) {
                setTitle(R.string.account_delete_confirmation_title)
                setMessage(R.string.account_delete_confirmation_description)
            }
        }
    }

    override fun itemClicked(accountModel: MetaAccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun deleteClicked(accountModel: MetaAccountUi) {
        viewModel.deleteClicked(accountModel)
    }
}
