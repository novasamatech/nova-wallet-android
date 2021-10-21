package io.novafoundation.nova.feature_staking_api.di

import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository

interface StakingFeatureApi {

    fun repository(): StakingRepository
}
