package io.novafoundation.nova.feature_staking_impl.di.staking

import io.novafoundation.nova.core.updater.Updater

class StakingUpdaters(val updaters: List<Updater<*>>)

fun StakingUpdaters(vararg updaters: Updater<*>) = StakingUpdaters(listOf(*updaters))
