package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.progress.observeProgressDialog
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.input.selector.setupListSelectorMixin
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_backup_settings.backupSettingsManualBtn
import kotlinx.android.synthetic.main.fragment_backup_settings.backupSettingsSwitcher
import kotlinx.android.synthetic.main.fragment_backup_settings.backupSettingsToolbar
import kotlinx.android.synthetic.main.fragment_backup_settings.backupStateView

class BackupSettingsFragment : BaseFragment<BackupSettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_backup_settings, container, false)
    }

    override fun initViews() {
        backupSettingsToolbar.applyStatusBarInsets()

        backupSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        backupStateView.setOnClickListener { viewModel.cloudBackupManageClicked() }
        backupStateView.setProblemClickListener() { viewModel.problemButtonClicked() }
        backupSettingsSwitcher.setOnClickListener { viewModel.backupSwitcherClicked() }
        backupSettingsManualBtn.setOnClickListener { viewModel.manualBackupClicked() }
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
        observeProgressDialog(viewModel.progressDialogMixin)
        observeActionBottomSheet(viewModel)
        setupListSelectorMixin(viewModel.listSelectorMixin)
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.confirmationAwaitableAction)

        viewModel.cloudBackupEnabled.observe { enabled ->
            backupSettingsSwitcher.setChecked(enabled)
        }

        viewModel.cloudBackupStateModel.observe { state ->
            backupStateView.setState(state)
        }
    }
}
