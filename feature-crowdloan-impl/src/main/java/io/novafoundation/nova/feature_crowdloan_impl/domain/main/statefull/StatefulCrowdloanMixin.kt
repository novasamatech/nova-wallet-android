package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.GroupedCrowdloans
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface StatefulCrowdloanMixin {

    interface Factory {

        fun create(scope: CoroutineScope): StatefulCrowdloanMixin
    }

    class ContributionsInfo(
        val contributionsCount: Int,
        val isUserHasContributions: Boolean,
        val totalContributed: AmountModel
    )

    val contributionsInfoFlow: Flow<LoadingState<ContributionsInfo>>

    val groupedCrowdloansFlow: Flow<LoadingState<GroupedCrowdloans>>
}
