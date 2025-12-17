package io.novafoundation.nova.feature_versions_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager

interface VersionsFeatureDependencies {

    fun resourceManager(): ResourceManager

    fun networkApiCreator(): NetworkApiCreator

    fun imageLoader(): ImageLoader

    fun preferences(): Preferences

    fun context(): Context

    fun appVersionProvider(): AppVersionProvider
}
