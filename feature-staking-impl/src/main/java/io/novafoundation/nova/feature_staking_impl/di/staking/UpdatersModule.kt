package io.novafoundation.nova.feature_staking_impl.di.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.Parachain
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.ParachainStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.Relaychain
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.RelaychainStakingUpdatersModule
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [RelaychainStakingUpdatersModule::class, ParachainStakingUpdatersModule::class])
class UpdatersModule {

    @Provides
    @FeatureScope
    fun provideStakingUpdateSystem(
        @Relaychain relaychainUpdaters: List<@JvmSuppressWildcards Updater>,
        @Parachain parachainUpdaters: List<@JvmSuppressWildcards Updater>,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: StakingSharedState
    ) = StakingUpdateSystem(
        relaychainUpdaters = relaychainUpdaters,
        parachainUpdaters = parachainUpdaters,
        chainRegistry = chainRegistry,
        singleAssetSharedState = singleAssetSharedState
    )
}
