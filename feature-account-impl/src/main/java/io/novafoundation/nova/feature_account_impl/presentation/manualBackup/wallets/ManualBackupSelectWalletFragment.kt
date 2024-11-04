package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.wallets

import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentManualBackupSelectWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject

class ManualBackupSelectWalletFragment : BaseFragment<ManualBackupSelectWalletViewModel, FragmentManualBackupSelectWalletBinding>(), AccountHolder.AccountItemHandler {

    override fun createBinding() = FragmentManualBackupSelectWalletBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy {
        TextAdapter(
            requireContext().getString(R.string.manual_backup_select_wallet_header),
            R.style.TextAppearance_NovaFoundation_Bold_Title3
        )
    }

    private val listAdapter by lazy {
        ManualBackupAccountsAdapter(
            imageLoader = imageLoader,
            itemHandler = this
        )
    }

    private val adapter by lazy {
        ConcatAdapter(
            headerAdapter,
            listAdapter
        )
    }

    override fun initViews() {
        binder.manualBackupWalletsToolbar.applyStatusBarInsets()
        binder.manualBackupWalletsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.manualBackupWalletsList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .manualBackupSelectWallet()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupSelectWalletViewModel) {
        viewModel.walletsUI.observe {
            listAdapter.submitList(it)
        }
    }

    override fun itemClicked(accountModel: AccountUi) {
        viewModel.walletClicked(accountModel)
    }
}
