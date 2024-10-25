package io.novafoundation.nova.runtime.di

import android.content.Context
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.repository.AssetsIconModeService
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novasama.substrate_sdk_android.wsrpc.SocketService

interface RuntimeDependencies {

    fun networkApiCreator(): NetworkApiCreator

    fun socketServiceCreator(): SocketService

    fun gson(): Gson

    fun preferences(): Preferences

    fun fileProvider(): FileProvider

    fun context(): Context

    fun storageDao(): StorageDao

    fun chainDao(): ChainDao

    fun chainAssetDao(): ChainAssetDao

    fun assetsIconModeService(): AssetsIconModeService
}
