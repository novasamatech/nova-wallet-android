package io.novafoundation.nova.feature_versions_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences

interface VersionsFeatureDependencies {

    fun networkApiCreator(): NetworkApiCreator

    fun imageLoader(): ImageLoader

    fun preferences(): Preferences

    fun context(): Context
}
