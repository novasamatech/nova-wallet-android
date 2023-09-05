package io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking

import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.RealNominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.RealStartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.CompoundStakingTypeDetailsProvidersFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.RelaychainStakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.pools.PoolStakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.MultiSingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.EditingStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.pool.RealStakingTypeDetailsProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingTargetSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.RealMultiStakingTargetSelectionFormatter
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@MapKey
annotation class StakingTypeGroupKey(val group: StakingTypeGroup)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@MapKey
annotation class StakingTypeProviderKey(val group: StakingTypeGroup)

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class MultiStakingSelectionStoreProviderKey()

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class StakingTypeEditingStoreProviderKey()

@Module
class StartMultiStakingModule {

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeProviderKey(StakingTypeGroup.NOMINATION_POOL)
    fun providePoolStakingTypeDetailsProviderFactory(
        poolStakingTypeDetailsInteractorFactory: PoolStakingTypeDetailsInteractorFactory,
        singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
        @MultiStakingSelectionStoreProviderKey currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider
    ): StakingTypeDetailsProviderFactory {
        return RealStakingTypeDetailsProviderFactory(
            poolStakingTypeDetailsInteractorFactory,
            singleStakingPropertiesFactory,
            currentSelectionStoreProvider
        )
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeProviderKey(StakingTypeGroup.RELAYCHAIN)
    fun provideRelaychainDirectStakingTypeDetailsProviderFactory(
        relaychainStakingTypeDetailsInteractorFactory: RelaychainStakingTypeDetailsInteractorFactory,
        singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
        @MultiStakingSelectionStoreProviderKey currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider
    ): StakingTypeDetailsProviderFactory {
        return RealStakingTypeDetailsProviderFactory(
            relaychainStakingTypeDetailsInteractorFactory,
            singleStakingPropertiesFactory,
            currentSelectionStoreProvider
        )
    }

    @Provides
    @FeatureScope
    fun provideCompoundStakingTypeDetailsProvidersFactory(
        factories: Map<StakingTypeGroup, @JvmSuppressWildcards StakingTypeDetailsProviderFactory>,
    ): CompoundStakingTypeDetailsProvidersFactory {
        return CompoundStakingTypeDetailsProvidersFactory(factories)
    }

    @Provides
    @FeatureScope
    fun provideSetupStakingTypeSelectionMixinFactory(
        @MultiStakingSelectionStoreProviderKey currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        @StakingTypeEditingStoreProviderKey editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    ): SetupStakingTypeSelectionMixinFactory {
        return SetupStakingTypeSelectionMixinFactory(
            currentSelectionStoreProvider,
            editableSelectionStoreProvider
        )
    }

    @Provides
    @FeatureScope
    fun provideEditingStakingTypeSelectionMixinFactory(
        @MultiStakingSelectionStoreProviderKey currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        @StakingTypeEditingStoreProviderKey editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        compoundStakingTypeDetailsProvidersFactory: CompoundStakingTypeDetailsProvidersFactory
    ): EditingStakingTypeSelectionMixinFactory {
        return EditingStakingTypeSelectionMixinFactory(
            currentSelectionStoreProvider,
            editableSelectionStoreProvider,
            compoundStakingTypeDetailsProvidersFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideMultiStakingSelectionFormatter(
        resourceManager: ResourceManager,
        poolDisplayFormatter: PoolDisplayFormatter,
    ): MultiStakingTargetSelectionFormatter {
        return RealMultiStakingTargetSelectionFormatter(resourceManager, poolDisplayFormatter)
    }

    @Provides
    @FeatureScope
    @MultiStakingSelectionStoreProviderKey
    fun provideStartMultiStakingSelectionStoreProvider(
        computationalCache: ComputationalCache
    ): StartMultiStakingSelectionStoreProvider {
        return RealStartMultiStakingSelectionStoreProvider(computationalCache, "MultiStakingSelection")
    }

    @Provides
    @FeatureScope
    @StakingTypeEditingStoreProviderKey
    fun provideStakingTypeEditingSelectionStoreProvider(
        computationalCache: ComputationalCache
    ): StartMultiStakingSelectionStoreProvider {
        return RealStartMultiStakingSelectionStoreProvider(computationalCache, "StakingTypeEditing")
    }

    @Provides
    @FeatureScope
    fun provideNominationPoolsAvailableBalanceResolver(
        walletConstants: WalletConstants
    ): NominationPoolsAvailableBalanceResolver {
        return RealNominationPoolsAvailableBalanceResolver(walletConstants)
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeGroupKey(StakingTypeGroup.RELAYCHAIN)
    fun provideDirectStakingPropertiesFactory(
        validatorRecommenderFactory: ValidatorRecommenderFactory,
        recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
        stakingSharedComputation: StakingSharedComputation,
        stakingRepository: StakingRepository
    ): SingleStakingPropertiesFactory {
        return DirectStakingPropertiesFactory(
            validatorRecommenderFactory = validatorRecommenderFactory,
            recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
            stakingSharedComputation = stakingSharedComputation,
            stakingRepository = stakingRepository
        )
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeGroupKey(StakingTypeGroup.NOMINATION_POOL)
    fun providePoolsStakingPropertiesFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
        availableBalanceResolver: NominationPoolsAvailableBalanceResolver,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
    ): SingleStakingPropertiesFactory {
        return NominationPoolStakingPropertiesFactory(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            nominationPoolRecommenderFactory = nominationPoolRecommenderFactory,
            poolsAvailableBalanceResolver = availableBalanceResolver,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingPropertiesFactory(
        creators: Map<StakingTypeGroup, @JvmSuppressWildcards SingleStakingPropertiesFactory>,
    ): SingleStakingPropertiesFactory {
        return MultiSingleStakingPropertiesFactory(creators)
    }

    @Provides
    @FeatureScope
    fun provideMultiStakingSelectionTypeProviderFactory(
        @MultiStakingSelectionStoreProviderKey selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
        chainRegistry: ChainRegistry,
        locksRepository: BalanceLocksRepository
    ): MultiStakingSelectionTypeProviderFactory {
        return MultiStakingSelectionTypeProviderFactory(
            selectionStoreProvider = selectionStoreProvider,
            singleStakingPropertiesFactory = singleStakingPropertiesFactory,
            chainRegistry = chainRegistry,
            locksRepository = locksRepository
        )
    }
}
