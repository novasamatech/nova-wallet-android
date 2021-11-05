package io.novafoundation.nova.splash.di

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository

interface SplashFeatureDependencies {
    fun accountRepository(): AccountRepository
}
