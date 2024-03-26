package io.novafoundation.nova.feature_onboarding_api.domain

interface OnboardingInteractor {

    suspend fun connectToCloud(): Result<Unit>

    fun isCloudAvailable(): Boolean
}
