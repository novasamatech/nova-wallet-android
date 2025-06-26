package io.novafoundation.nova.splash.di

import io.novafoundation.nova.common.utils.splash.SplashPassedObserver
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

interface SplashFeatureDependencies {

    val splashPassedObserver: SplashPassedObserver

    fun accountRepository(): AccountRepository

    fun updateNotificationsInteractor(): UpdateNotificationsInteractor
}
