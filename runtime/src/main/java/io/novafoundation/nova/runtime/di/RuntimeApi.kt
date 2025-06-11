package io.novafoundation.nova.runtime.di

import com.google.gson.Gson
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.MortalityConstructor
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.RemoteToDomainChainMapperFacade
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeFilesCache
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.BlockLimitsRepository
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import io.novafoundation.nova.runtime.repository.ChainRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.repository.PreConfiguredChainsRepository
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtrinsicSerialization

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

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson

    fun runtimeVersionsRepository(): RuntimeVersionsRepository

    fun eventsRepository(): EventsRepository

    val multiChainQrSharingFactory: MultiChainQrSharingFactory

    val sampledBlockTime: SampledBlockTimeStorage

    val parachainInfoRepository: ParachainInfoRepository

    val mortalityConstructor: MortalityConstructor

    val extrinsicValidityUseCase: ExtrinsicValidityUseCase

    val timestampRepository: TimestampRepository

    val totalIssuanceRepository: TotalIssuanceRepository

    val storageStorageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory

    val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi

    val gasPriceProviderFactory: GasPriceProviderFactory

    val extrinsicWalk: ExtrinsicWalk

    val runtimeFilesCache: RuntimeFilesCache

    val metadataShortenerService: MetadataShortenerService

    val runtimeProviderPool: RuntimeProviderPool

    val nodeHealthStateTesterFactory: NodeHealthStateTesterFactory

    val chainNodeRepository: ChainNodeRepository

    val nodeConnectionFactory: NodeConnectionFactory

    val web3ApiFactory: Web3ApiFactory

    val preConfiguredChainsRepository: PreConfiguredChainsRepository

    val chainRepository: ChainRepository

    val remoteToDomainChainMapperFacade: RemoteToDomainChainMapperFacade

    val blockLimitsRepository: BlockLimitsRepository
}
