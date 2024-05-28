package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.CreateBackupPasswordFragment

class SyncWalletsBackupPasswordFragment : CreateBackupPasswordFragment<SyncWalletsBackupPasswordViewModel>() {

    override val titleRes: Int = R.string.create_cloud_backup_password_title
    override val subtitleRes: Int = R.string.create_cloud_backup_password_subtitle

    override fun initViews() {
        super.initViews()
        onBackPressed { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .syncWalletsBackupPasswordFactory()
            .create(this)
            .inject(this)
    }
}
