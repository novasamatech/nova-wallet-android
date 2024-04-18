package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.CreateBackupPasswordFragment

class ChangeBackupPasswordFragment : CreateBackupPasswordFragment<ChangeBackupPasswordViewModel>() {

    override val titleRes: Int = R.string.change_cloud_backup_password_title
    override val subtitleRes: Int = R.string.change_cloud_backup_password_subtitle

    override fun initViews() {
        super.initViews()
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .changeBackupPasswordComponentFactory()
            .create(this)
            .inject(this)
    }
}
