package io.novafoundation.nova.feature_onboarding_impl.di

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository

interface OnboardingFeatureDependencies {

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider
}
