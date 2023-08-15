package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module

@Module(
    includes = [
        ParachainStakingNavigationModule::class,
        RelayStakingNavigationModule::class,
        NominationPoolsStakingNavigationModule::class
    ]
)
class StakingNavigationModule
