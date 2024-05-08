package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.decoration.ExtraSpaceItemDecoration
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.ManualBackupSecretsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem
import kotlinx.android.synthetic.main.fragment_manual_backup_secrets.manualBackupSecretsList
import kotlinx.android.synthetic.main.fragment_manual_backup_secrets.manualBackupSecretsToolbar
import kotlinx.android.synthetic.main.fragment_manual_backup_select_wallet.manualBackupWalletsToolbar

class ManualBackupSecretsFragment : BaseFragment<ManualBackupSecretsViewModel>(), ManualBackupItemHandler {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupCommonPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    private val adapter = ManualBackupSecretsAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_backup_secrets, container, false)
    }

    override fun initViews() {
        manualBackupSecretsToolbar.applyStatusBarInsets()
        manualBackupSecretsToolbar.setHomeButtonListener { viewModel.backClicked() }

        manualBackupSecretsList.adapter = adapter
        manualBackupSecretsList.addItemDecoration(ExtraSpaceItemDecoration())
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .manualBackupSecrets()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupSecretsViewModel) {
        viewModel.walletModel.observe {
            manualBackupSecretsToolbar.setTitleIcon(it.icon)
            manualBackupSecretsToolbar.setTitle(it.name)
        }

        viewModel.exportList.observe {
            adapter.submitList(it)
        }
    }

    override fun onExportJsonClick(item: ManualBackupJsonRvItem) {
        showError("Not implemented yet")
    }

    override fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem) {
        viewModel.onTapToRevealClicked(item)
    }
}
