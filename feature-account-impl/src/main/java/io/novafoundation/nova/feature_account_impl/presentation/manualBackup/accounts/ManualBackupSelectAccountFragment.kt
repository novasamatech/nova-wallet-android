package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts

import android.os.Bundle
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentManualBackupSelectWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountsAdapter
import javax.inject.Inject

class ManualBackupSelectAccountFragment : BaseFragment<ManualBackupSelectAccountViewModel, FragmentManualBackupSelectWalletBinding>(), ManualBackupAccountsAdapter.AccountHandler {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupSelectAccountPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentManualBackupSelectWalletBinding::bind)

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
            .manualBackupSelectAccount()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupSelectAccountViewModel) {
        viewModel.walletModel.observe {
            binder.manualBackupWalletsToolbar.setTitleIcon(it.icon)
            binder.manualBackupWalletsToolbar.setTitle(it.name)
        }

        viewModel.accountsList.observe {
            listAdapter.submitList(it)
        }
    }

    override fun onAccountClicked(account: ManualBackupAccountRvItem) {
        viewModel.walletClicked(account)
    }
}
