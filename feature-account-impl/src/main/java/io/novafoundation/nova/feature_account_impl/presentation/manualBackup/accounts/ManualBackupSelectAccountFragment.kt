package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts

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
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountRVItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountsAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_manual_backup_select_wallet.manualBackupWalletsList
import kotlinx.android.synthetic.main.fragment_manual_backup_select_wallet.manualBackupWalletsToolbar

class ManualBackupSelectAccountFragment : BaseFragment<ManualBackupSelectAccountViewModel>(), ManualBackupAccountsAdapter.AccountHandler {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupSelectAccountPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy {
        TextAdapter(
            requireContext().getString(R.string.manual_backup_select_account_header),
            R.style.TextAppearance_NovaFoundation_Bold_Title3
        )
    }

    private val listAdapter by lazy {
        ManualBackupAccountsAdapter(
            imageLoader = imageLoader,
            accountHandler = this
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
            .manualBackupSelectAccount()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupSelectAccountViewModel) {
        viewModel.walletModel.observe {
            manualBackupWalletsToolbar.setTitleIcon(it.icon)
            manualBackupWalletsToolbar.setTitle(it.name)
        }

        viewModel.accountsList.observe {
            listAdapter.submitList(it)
        }
    }

    override fun onAccountClicked(account: ManualBackupAccountRVItem) {
        viewModel.walletClicked(account)
    }
}
