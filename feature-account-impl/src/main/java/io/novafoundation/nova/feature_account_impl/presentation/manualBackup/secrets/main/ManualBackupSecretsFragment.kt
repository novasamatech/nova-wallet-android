package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.main

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.decoration.ExtraSpaceItemDecoration
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentManualBackupSecretsBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupSecretsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

class ManualBackupSecretsFragment : BaseFragment<ManualBackupSecretsViewModel, FragmentManualBackupSecretsBinding>(), ManualBackupItemHandler {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupCommonPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun createBinding() = FragmentManualBackupSecretsBinding.inflate(layoutInflater)

    private val adapter = ManualBackupSecretsAdapter(this)

    override fun initViews() {
        binder.manualBackupSecretsToolbar.applyStatusBarInsets()
        binder.manualBackupSecretsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.manualBackupSecretsToolbar.setRightActionClickListener { viewModel.advancedSecretsClicked() }

        binder.manualBackupSecretsList.adapter = adapter
        binder.manualBackupSecretsList.addItemDecoration(ExtraSpaceItemDecoration())
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
            binder.manualBackupSecretsToolbar.setTitleIcon(it.icon)
            binder.manualBackupSecretsToolbar.setTitle(it.name)
        }

        viewModel.advancedSecretsBtnAvailable.observe { available ->
            if (available) {
                binder.manualBackupSecretsToolbar.setRightIconRes(R.drawable.ic_options)
            } else {
                binder.manualBackupSecretsToolbar.hideRightAction()
            }
        }

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
