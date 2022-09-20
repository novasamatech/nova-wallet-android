package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.core.updater.Updater
import kotlinx.coroutines.flow.Flow

interface ContributionsInteractor {
    fun runUpdate(): Flow<Updater.SideEffect>

    fun observeChainContributions(): Flow<ContributionsWithTotalAmount>
}
