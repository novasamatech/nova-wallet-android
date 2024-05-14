package io.novafoundation.nova.feature_onboarding_impl.domain

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor

class OnboardingInteractorImpl(
    private val accountRepository: AccountRepository,
    private val cloudBackupService: CloudBackupService
) : OnboardingInteractor {

    override suspend fun checkCloudBackupIsExist(): Result<Boolean> {
        return cloudBackupService.isCloudBackupExist()
    }

    override suspend fun isCloudBackupAvailableForImport(): Boolean {
        val syncWithCloudNotEnabled = !cloudBackupService.session.isSyncWithCloudEnabled()
        val withoutAddedAccounts = !accountRepository.hasActiveMetaAccounts()
        return syncWithCloudNotEnabled && withoutAddedAccounts
    }

    override suspend fun signInToCloud(): Result<Unit> {
        return cloudBackupService.signInToCloud()
    }
}
