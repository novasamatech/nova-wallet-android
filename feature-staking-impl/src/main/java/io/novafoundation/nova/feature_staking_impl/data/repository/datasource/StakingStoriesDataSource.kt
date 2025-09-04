package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.feature_staking_api.domain.model.StakingStory
import kotlinx.coroutines.flow.Flow

interface StakingStoriesDataSource {

    fun getStoriesFlow(): Flow<List<StakingStory>>
}
