package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordFragment

class CheckCloudBackupPasswordFragment : EnterCloudBackupPasswordFragment<CheckCloudBackupPasswordViewModel>() {

    override val titleRes: Int = R.string.confirm_cloud_backup_password_title
    override val subtitleRes: Int = R.string.confirm_cloud_backup_password_subtitle

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .checkCloudBackupPasswordFactory()
            .create(this)
            .inject(this)
    }
}
