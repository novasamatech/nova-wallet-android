package io.novafoundation.nova.feature_swap_core.di

import com.google.gson.Gson
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface SwapCoreDependencies {

    val chainRegistry: ChainRegistry

    val chainStateRepository: ChainStateRepository

    val gson: Gson

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    val computationalCache: ComputationalCache

}
