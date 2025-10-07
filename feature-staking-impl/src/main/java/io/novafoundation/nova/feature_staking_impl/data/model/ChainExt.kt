package io.novafoundation.nova.feature_staking_impl.data.model

import io.novafoundation.nova.runtime.ext.allExternalApis
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun Chain.stakingRewardsExternalApi(): List<Chain.ExternalApi.StakingRewards> = allExternalApis<Chain.ExternalApi.StakingRewards>()

fun Chain.stakingExternalApi(): Chain.ExternalApi.Staking? = externalApi<Chain.ExternalApi.Staking>()
