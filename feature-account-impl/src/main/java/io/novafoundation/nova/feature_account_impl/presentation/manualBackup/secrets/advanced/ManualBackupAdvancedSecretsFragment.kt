package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced

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
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupSecretsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

class ManualBackupAdvancedSecretsFragment : BaseFragment<ManualBackupAdvancedSecretsViewModel, FragmentManualBackupAdvancedSecretsBinding>(), ManualBackupItemHandler {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupCommonPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentManualBackupAdvancedSecretsBinding::bind)

    private val adapter = ManualBackupSecretsAdapter(this)

    override fun initViews() {
        binder.manualBackupAdvancedSecretsToolbar.applyStatusBarInsets()
        binder.manualBackupAdvancedSecretsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.manualBackupAdvancedSecretsList.adapter = adapter
        binder.manualBackupAdvancedSecretsList.addItemDecoration(ExtraSpaceItemDecoration())
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .manualBackupAdvancedSecrets()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupAdvancedSecretsViewModel) {
        viewModel.exportList.observe {
            adapter.submitList(it)
        }
    }

    override fun onExportJsonClick(item: ManualBackupJsonRvItem) {
        viewModel.exportJsonClicked()
    }

    override fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem) {
        viewModel.onTapToRevealClicked(item)
    }
}
