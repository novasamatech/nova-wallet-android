package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordFragment

class RestoreCloudBackupPasswordFragment : EnterCloudBackupPasswordFragment<RestoreCloudBackupPasswordViewModel>() {

    override val titleRes: Int = R.string.restore_cloud_backup_title
    override val subtitleRes: Int = R.string.restore_cloud_backup_password_title

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .restoreCloudBackupPasswordFactory()
            .create(this)
            .inject(this)
    }
}
