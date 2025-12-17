package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.setupCustomDialogDisplayer
import io.novafoundation.nova.common.utils.progress.observeProgressDialog
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.input.selector.setupListSelectorMixin
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentBackupSettingsBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.CloudBackupDiffBottomSheet

class BackupSettingsFragment : BaseFragment<BackupSettingsViewModel, FragmentBackupSettingsBinding>() {

    override fun createBinding() = FragmentBackupSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.backupSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.backupStateView.setOnClickListener { viewModel.cloudBackupManageClicked() }
        binder.backupStateView.setProblemClickListener() { viewModel.problemButtonClicked() }
        binder.backupSettingsSwitcher.setOnClickListener { viewModel.backupSwitcherClicked() }
        binder.backupSettingsManualBtn.setOnClickListener { viewModel.manualBackupClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .backupSettings()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BackupSettingsViewModel) {
        setupCustomDialogDisplayer(viewModel)
        observeProgressDialog(viewModel.progressDialogMixin)
        observeActionBottomSheet(viewModel)
        setupListSelectorMixin(viewModel.listSelectorMixin)
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.negativeConfirmationAwaitableAction)
        setupConfirmationDialog(R.style.AccentAlertDialogTheme, viewModel.neutralConfirmationAwaitableAction)

        viewModel.cloudBackupEnabled.observe { enabled ->
            binder.backupSettingsSwitcher.setChecked(enabled)
        }

        viewModel.cloudBackupStateModel.observe { state ->
            binder.backupStateView.setState(state)
        }

        viewModel.cloudBackupChangesLiveData.observeEvent {
            showBackupDiffBottomSheet(it)
        }
    }

    private fun showBackupDiffBottomSheet(payload: CloudBackupDiffBottomSheet.Payload) {
        val bottomSheet = CloudBackupDiffBottomSheet(
            requireContext(),
            payload,
            onApply = { diff, cloudBackup -> viewModel.applyBackupDestructiveChanges(diff, cloudBackup) }
        )

        bottomSheet.show()
    }
}
