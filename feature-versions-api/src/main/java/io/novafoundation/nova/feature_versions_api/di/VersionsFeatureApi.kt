package io.novafoundation.nova.feature_versions_api.di

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

interface VersionsFeatureApi {

    fun interactor(): UpdateNotificationsInteractor
}
