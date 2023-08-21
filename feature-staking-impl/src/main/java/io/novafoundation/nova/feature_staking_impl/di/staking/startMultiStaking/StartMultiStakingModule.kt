package io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking

import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.RealStartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.MultiSingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.RealMultiStakingSelectionFormatter
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@MapKey
annotation class StakingTypeGroupKey(val group: StakingTypeGroup)

@Module
class StartMultiStakingModule {

    @Provides
    @FeatureScope
    fun provideMultiStakingSelectionFormatter(
        resourceManager: ResourceManager,
        poolDisplayFormatter: PoolDisplayFormatter,
    ): MultiStakingSelectionFormatter {
        return RealMultiStakingSelectionFormatter(resourceManager, poolDisplayFormatter)
    }

    @Provides
    @FeatureScope
    fun provideStartMultiStakingSelectionStoreProvider(
        computationalCache: ComputationalCache
    ): StartMultiStakingSelectionStoreProvider {
        return RealStartMultiStakingSelectionStoreProvider(computationalCache)
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeGroupKey(StakingTypeGroup.RELAYCHAIN)
    fun provideDirectStakingPropertiesFactory(
        validatorRecommendatorFactory: ValidatorRecommendatorFactory,
        recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
        stakingSharedComputation: StakingSharedComputation,
    ): SingleStakingPropertiesFactory {
        return DirectStakingPropertiesFactory(
            validatorRecommendatorFactory = validatorRecommendatorFactory,
            recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
            stakingSharedComputation = stakingSharedComputation
        )
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeGroupKey(StakingTypeGroup.NOMINATION_POOL)
    fun providePoolsStakingPropertiesFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation
    ): SingleStakingPropertiesFactory {
        return NominationPoolStakingPropertiesFactory(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
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
        selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
        chainRegistry: ChainRegistry,
    ): MultiStakingSelectionTypeProviderFactory {
        return MultiStakingSelectionTypeProviderFactory(
            selectionStoreProvider = selectionStoreProvider,
            singleStakingPropertiesFactory = singleStakingPropertiesFactory,
            chainRegistry = chainRegistry
        )
    }
}
