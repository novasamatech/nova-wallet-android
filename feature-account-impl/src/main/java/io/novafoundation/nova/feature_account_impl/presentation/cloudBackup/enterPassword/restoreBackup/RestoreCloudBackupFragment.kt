package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restoreBackup

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordFragment

class RestoreCloudBackupFragment : EnterCloudBackupPasswordFragment<RestoreCloudBackupViewModel>() {

    override val titleRes: Int = R.string.restore_cloud_backup_title
    override val subtitleRes: Int = R.string.restore_cloud_backup_subtitle

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .restoreCloudBackupFactory()
            .create(this)
            .inject(this)
    }
}
