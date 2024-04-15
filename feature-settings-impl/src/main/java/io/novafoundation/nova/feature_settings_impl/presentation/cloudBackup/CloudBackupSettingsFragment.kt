package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsManualBtn
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsSwitcher
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsToolbar
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupStateView

class CloudBackupSettingsFragment : BaseFragment<CloudBackupSettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_cloud_backup_settings, container, false)
    }

    override fun initViews() {
        cloudBackupSettingsToolbar.applyStatusBarInsets()

        cloudBackupSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        cloudBackupStateView.setOnClickListener { viewModel.cloudBackupManageClicked() }
        cloudBackupStateView.setProblemClickListener() { viewModel.problemButtonClicked() }
        cloudBackupSettingsSwitcher.setOnClickListener { viewModel.backupSwitcherClicked() }
        cloudBackupSettingsManualBtn.setOnClickListener { viewModel.manualBackupClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .cloudBackupSettings()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CloudBackupSettingsViewModel) {
        viewModel.cloudBackupEnabled.observe { enabled ->
            cloudBackupSettingsSwitcher.setChecked(enabled)
        }

        viewModel.cloudBackupStateModel.observe { state ->
            cloudBackupStateView.setState(state)
        }
    }
}
