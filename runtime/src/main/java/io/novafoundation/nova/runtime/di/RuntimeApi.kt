package io.novafoundation.nova.runtime.di

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Named

interface RuntimeApi {

    fun provideExtrinsicBuilderFactory(): ExtrinsicBuilderFactory

    fun externalRequirementFlow(): MutableStateFlow<ChainConnection.ExternalRequirement>

    fun storageCache(): StorageCache

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    @Named(LOCAL_STORAGE_SOURCE)
    fun localStorageSource(): StorageDataSource

    fun chainSyncService(): ChainSyncService

    fun chainStateRepository(): ChainStateRepository

    fun chainRegistry(): ChainRegistry

    fun rpcCalls(): RpcCalls
}
