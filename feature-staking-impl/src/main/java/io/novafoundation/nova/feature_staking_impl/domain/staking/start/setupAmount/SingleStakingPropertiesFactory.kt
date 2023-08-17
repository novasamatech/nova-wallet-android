package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import kotlinx.coroutines.CoroutineScope

interface SingleStakingPropertiesFactory {

    fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties
}

class MultiSingleStakingPropertiesFactory(
    private val creators: Map<StakingTypeGroup, SingleStakingPropertiesFactory>
): SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        val stakingGroup = stakingOption.additional.stakingType.group()

        return creators.getValue(stakingGroup).createProperties(scope, stakingOption)
    }
}
