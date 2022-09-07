package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.GroupedCrowdloans
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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

    val selectedAccount: Flow<MetaAccount>
    val selectedChain: Flow<Chain>

    val contributionsInfoFlow: Flow<LoadingState<ContributionsInfo>>

    val groupedCrowdloansFlow: Flow<LoadingState<GroupedCrowdloans>>
}
