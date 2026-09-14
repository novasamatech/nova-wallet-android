package io.novafoundation.nova.analytics.di

import android.content.Context
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.infrastructure.di.Attested

interface AnalyticsFeatureDependencies {

    fun context(): Context

    fun gson(): Gson

    fun preferences(): Preferences

    fun rootScope(): RootScope

    @Attested
    fun attestedNetworkApiCreator(): NetworkApiCreator

    fun analyticsEventsDao(): AnalyticsEventsDao
}
