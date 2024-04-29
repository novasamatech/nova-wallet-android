package io.novafoundation.nova.feature_account_impl.presentation.manualBackup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_manual_backup_select_wallet.manualBackupWalletsList
import kotlinx.android.synthetic.main.fragment_manual_backup_select_wallet.manualBackupWalletsToolbar

class ManualBackupSelectWalletFragment : BaseFragment<ManualBackupSelectWalletViewModel>(), AccountHolder.AccountItemHandler {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_backup_select_wallet, container, false)
    }

    override fun initViews() {
        manualBackupWalletsToolbar.applyStatusBarInsets()
        manualBackupWalletsToolbar.setHomeButtonListener { viewModel.backClicked() }

        manualBackupWalletsList.adapter = adapter
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
