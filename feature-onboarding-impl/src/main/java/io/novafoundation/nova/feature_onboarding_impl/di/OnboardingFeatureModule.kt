package io.novafoundation.nova.feature_onboarding_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor
import io.novafoundation.nova.feature_onboarding_impl.domain.OnboardingInteractorImpl

@Module
class OnboardingFeatureModule {

    @Provides
    fun provideOnboardingInteractor(cloudBackupService: CloudBackupService): OnboardingInteractor {
        return OnboardingInteractorImpl(cloudBackupService)
    }
}
