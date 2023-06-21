package io.novafoundation.nova.feature_crowdloan_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parachain.ParachainMetadataApi
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.CrowdloanRepositoryImpl
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeModule
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.CrowdloanContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_wallet_api.di.common.SelectableAssetUseCaseModule
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(
    includes = [
        CustomContributeModule::class,
        SelectableAssetUseCaseModule::class,
    ]
)
class CrowdloanFeatureModule {

    @Provides
    @FeatureScope
    fun provideCrowdloanSharedState(
        chainRegistry: ChainRegistry,
        preferences: Preferences,
    ) = CrowdloanSharedState(chainRegistry, preferences)

    @Provides
    @FeatureScope
    fun provideSelectableSharedState(crowdloanSharedState: CrowdloanSharedState): SelectableSingleAssetSharedState<*> = crowdloanSharedState

    @Provides
    @FeatureScope
    fun provideFeeLoaderMixin(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        tokenUseCase: TokenUseCase,
    ): FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(tokenUseCase)

    @Provides
    @FeatureScope
    fun crowdloanRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        crowdloanMetadataApi: ParachainMetadataApi,
        chainRegistry: ChainRegistry,
    ): CrowdloanRepository = CrowdloanRepositoryImpl(
        remoteStorageSource,
        chainRegistry,
        crowdloanMetadataApi
    )

    @Provides
    @FeatureScope
    fun provideCrowdloanInteractor(
        crowdloanRepository: CrowdloanRepository,
        chainStateRepository: ChainStateRepository,
        contributionsRepository: ContributionsRepository
    ) = CrowdloanInteractor(
        crowdloanRepository,
        chainStateRepository,
        contributionsRepository
    )

    @Provides
    @FeatureScope
    fun provideCrowdloanMetadataApi(networkApiCreator: NetworkApiCreator): ParachainMetadataApi {
        return networkApiCreator.create(ParachainMetadataApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideCrowdloanContributeInteractor(
        extrinsicService: ExtrinsicService,
        accountRepository: AccountRepository,
        chainStateRepository: ChainStateRepository,
        sharedState: CrowdloanSharedState,
        crowdloanRepository: CrowdloanRepository,
        customContributeManager: CustomContributeManager,
        contributionsRepository: ContributionsRepository
    ) = CrowdloanContributeInteractor(
        extrinsicService,
        accountRepository,
        chainStateRepository,
        customContributeManager,
        sharedState,
        crowdloanRepository,
        contributionsRepository
    )
}
