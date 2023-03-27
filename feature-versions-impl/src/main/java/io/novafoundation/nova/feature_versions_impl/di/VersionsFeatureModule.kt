package io.novafoundation.nova.feature_versions_impl.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.AppVersionProvider
import io.novafoundation.nova.feature_versions_impl.data.PackageManagerAppVersionProvider
import io.novafoundation.nova.feature_versions_impl.data.RealVersionRepository
import io.novafoundation.nova.feature_versions_impl.data.VersionRepository
import io.novafoundation.nova.feature_versions_impl.data.VersionsFetcher
import io.novafoundation.nova.feature_versions_impl.domain.RealUpdateNotificationsInteractor

@Module
class VersionsFeatureModule {

    @Provides
    fun provideVersionsFetcher(
        networkApiCreator: NetworkApiCreator,
    ) = networkApiCreator.create(VersionsFetcher::class.java)

    @Provides
    fun provideAppVersionProvider(context: Context): AppVersionProvider = PackageManagerAppVersionProvider(context)

    @Provides
    fun provideVersionService(
        appVersionProvider: AppVersionProvider,
        preferences: Preferences,
        versionsFetcher: VersionsFetcher
    ): VersionRepository = RealVersionRepository(appVersionProvider, preferences, versionsFetcher)

    @Provides
    @FeatureScope
    fun provideUpdateNotificationsInteractor(
        versionRepository: VersionRepository
    ): UpdateNotificationsInteractor = RealUpdateNotificationsInteractor(versionRepository)
}
