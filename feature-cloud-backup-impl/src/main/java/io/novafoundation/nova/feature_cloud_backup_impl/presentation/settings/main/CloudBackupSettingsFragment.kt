package io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_cloud_backup_impl.R
import io.novafoundation.nova.feature_cloud_backup_impl.di.CloudBackupFeatureComponent
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsManualBtn
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsSwitcher
import kotlinx.android.synthetic.main.fragment_cloud_backup_settings.cloudBackupSettingsToolbar

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
        cloudBackupSettingsSwitcher.setOnClickListener { viewModel.backupSwitcherClicked() }
        cloudBackupSettingsManualBtn.setOnClickListener { viewModel.manualBackupClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<CloudBackupFeatureComponent>(
            requireContext(),
            CloudBackupFeatureApi::class.java
        )
            .cloudBackupSettings()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CloudBackupSettingsViewModel) {
        viewModel.cloudBackupEnabled.observe { enabled ->
            cloudBackupSettingsSwitcher.setChecked(enabled)
        }
    }
}
