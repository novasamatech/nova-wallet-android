package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing.TuringAdditionalIssuanceUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class TuringStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideAdditionalIssuanceUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = TuringAdditionalIssuanceUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    @Turing
    fun provideTuringExtraUpdaters(
        turingAdditionalIssuanceUpdater: TuringAdditionalIssuanceUpdater
    ): List<Updater> = listOf(turingAdditionalIssuanceUpdater)
}
