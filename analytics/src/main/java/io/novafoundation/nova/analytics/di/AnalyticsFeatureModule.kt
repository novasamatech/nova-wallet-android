package io.novafoundation.nova.analytics.di

import android.content.Context
import com.google.gson.Gson
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.NoOpAnalyticsService
import io.novafoundation.nova.analytics.BuildConfig
import io.novafoundation.nova.analytics.RealAnalyticsOptOutManager
import io.novafoundation.nova.analytics.transport.AnalyticsApi
import io.novafoundation.nova.analytics.transport.AnalyticsEventQueue
import io.novafoundation.nova.analytics.transport.AnalyticsIdentity
import io.novafoundation.nova.analytics.transport.AnalyticsUploader
import io.novafoundation.nova.analytics.transport.RealAnalyticsIdentity
import io.novafoundation.nova.analytics.transport.RealAnalyticsService
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.coroutines.DangerousScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.infrastructure.di.Attested

private const val ANALYTICS_QUEUE_MAX_SIZE = 500

private const val ANALYTICS_BATCH_SIZE = 300

@Module
class AnalyticsFeatureModule {

    @Provides
    @ApplicationScope
    fun provideAnalyticsApi(@Attested networkApiCreator: NetworkApiCreator): AnalyticsApi {
        return networkApiCreator.create(AnalyticsApi::class.java, BuildConfig.ANALYTICS_HOST)
    }

    @Provides
    @ApplicationScope
    fun provideAnalyticsIdentity(preferences: Preferences): AnalyticsIdentity {
        return RealAnalyticsIdentity(preferences)
    }

    @Provides
    @ApplicationScope
    fun provideAnalyticsEventQueue(dao: AnalyticsEventsDao, gson: Gson): AnalyticsEventQueue {
        return AnalyticsEventQueue(dao, gson, ANALYTICS_QUEUE_MAX_SIZE)
    }

    @Provides
    @ApplicationScope
    fun provideAnalyticsUploader(
        context: Context,
        api: AnalyticsApi,
        identity: AnalyticsIdentity,
        queue: AnalyticsEventQueue
    ): AnalyticsUploader {
        return AnalyticsUploader(
            api = api,
            identity = identity,
            queue = queue,
            appVersion = context.appVersionName(),
            batchSize = ANALYTICS_BATCH_SIZE
        )
    }

    @OptIn(DangerousScope::class)
    @Provides
    @ApplicationScope
    fun provideAnalyticsService(
        rootScope: RootScope,
        queue: AnalyticsEventQueue,
        uploader: Lazy<AnalyticsUploader>,
        identity: AnalyticsIdentity
    ): AnalyticsService {
        if (BuildConfig.ANALYTICS_HOST.isBlank()) return NoOpAnalyticsService()

        return RealAnalyticsService(rootScope, queue, uploader.get(), identity, ANALYTICS_BATCH_SIZE)
    }

    @Provides
    @ApplicationScope
    fun provideAnalyticsOptOutManager(
        preferences: Preferences,
        analyticsService: AnalyticsService
    ): AnalyticsOptOutManager {
        return RealAnalyticsOptOutManager(preferences, analyticsService)
    }
}

private fun Context.appVersionName(): String {
    return runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
}
