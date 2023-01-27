package io.novafoundation.nova.splash.di

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

interface SplashFeatureDependencies {
    fun accountRepository(): AccountRepository

    fun updateNotificationsInteractor(): UpdateNotificationsInteractor
}
