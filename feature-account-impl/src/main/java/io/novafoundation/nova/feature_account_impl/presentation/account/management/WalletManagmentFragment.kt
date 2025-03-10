package io.novafoundation.nova.feature_account_impl.presentation.account.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.input.selector.setupListSelectorMixin
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.observeConfirmationAction
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_accounts.accountListToolbar
import kotlinx.android.synthetic.main.fragment_accounts.accountsList
import kotlinx.android.synthetic.main.fragment_accounts.addAccount

class WalletManagmentFragment : BaseFragment<WalletManagmentViewModel>(), AccountHolder.AccountItemHandler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private lateinit var adapter: AccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = layoutInflater.inflate(R.layout.fragment_accounts, container, false)

    override fun initViews() {
        adapter = AccountsAdapter(
            this,
            imageLoader,
            initialMode = viewModel.mode.value,
            chainBorderColor = R.color.secondary_screen_background
        )

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
        observeConfirmationAction(viewModel.cloudBackupChangingWarningMixin)
        setupListSelectorMixin(viewModel.listSelectorMixin)
        viewModel.walletsListingMixin.metaAccountsFlow.observe(adapter::submitList)
        viewModel.mode.observe(adapter::setMode)

        viewModel.toolbarAction.observe(accountListToolbar::setTextRight)

        viewModel.confirmAccountDeletion.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                positiveTextRes = R.string.account_delete_confirm
            ) {
                setTitle(R.string.account_delete_confirmation_title)
                setMessage(R.string.account_delete_confirmation_description)
            }
        }
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.accountClicked(accountModel)
    }

    override fun deleteClicked(accountModel: AccountUi) {
        viewModel.deleteClicked(accountModel)
    }
}
