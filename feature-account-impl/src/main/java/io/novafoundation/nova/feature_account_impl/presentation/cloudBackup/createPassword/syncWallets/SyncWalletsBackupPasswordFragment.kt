package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.CreateBackupPasswordFragment

class SyncWalletsBackupPasswordFragment : CreateBackupPasswordFragment<SyncWalletsBackupPasswordViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_cloud_backup_password, container, false)
    }

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
