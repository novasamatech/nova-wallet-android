package io.novafoundation.nova.infrastructure.di

import android.content.Context
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.IntegrityService
import okhttp3.OkHttpClient

interface InfrastructureDependencies {

    fun context(): Context

    fun preferences(): Preferences

    fun okHttpClient(): OkHttpClient

    fun networkApiCreator(): NetworkApiCreator

    fun integrityService(): IntegrityService
}
