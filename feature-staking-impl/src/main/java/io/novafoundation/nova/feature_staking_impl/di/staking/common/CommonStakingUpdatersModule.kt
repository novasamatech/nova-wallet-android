package io.novafoundation.nova.feature_staking_impl.di.staking.common

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.common.network.blockhain.updaters.TotalIssuanceUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class CommonStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideTotalInsuranceUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = TotalIssuanceUpdater(
        sharedState,
        storageCache,
        chainRegistry
    )
}
