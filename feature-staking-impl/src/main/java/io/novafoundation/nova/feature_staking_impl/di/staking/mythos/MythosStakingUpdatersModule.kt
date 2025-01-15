package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters

@Module
class MythosStakingUpdatersModule {

    @Provides
    @FeatureScope
    @Mythos
    fun provideMythosStakingUpdaters(): StakingUpdaters.Group {
        return StakingUpdaters.Group(emptyList())
    }
}
