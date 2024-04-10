package io.novafoundation.nova.feature_onboarding_api.domain

interface OnboardingInteractor {

    suspend fun checkCloudBackupIsExist(): Result<Boolean>

    suspend fun isCloudBackupAvailableForImport(): Boolean
}
