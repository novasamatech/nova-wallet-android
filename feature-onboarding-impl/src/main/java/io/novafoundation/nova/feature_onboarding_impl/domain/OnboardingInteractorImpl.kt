package io.novafoundation.nova.feature_onboarding_impl.domain

import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor
import kotlinx.coroutines.delay

class OnboardingInteractorImpl : OnboardingInteractor {

    override suspend fun connectToCloud(): Result<Unit> {
        delay(4000) // Simulate network request
        TODO()
    }

    override fun isCloudAvailable(): Boolean {
        return true
    }
}
