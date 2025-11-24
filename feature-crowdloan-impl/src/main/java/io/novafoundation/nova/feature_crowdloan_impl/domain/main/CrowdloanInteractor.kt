package io.novafoundation.nova.feature_crowdloan_impl.domain.main

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias GroupedCrowdloans = GroupedList<KClass<out Crowdloan.State>, Crowdloan>

class CrowdloanInteractor(
    private val crowdloanRepository: CrowdloanRepository,
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository
) {

    fun groupedCrowdloansFlow(chain: Chain, account: MetaAccount): Flow<GroupedCrowdloans> {
        return crowdloansFlow(chain, account)
            .map { groupCrowdloans(it) }
    }

    private fun crowdloansFlow(chain: Chain, account: MetaAccount): Flow<List<Crowdloan>> {
        return flow {
            val accountId = account.accountIdIn(chain)

            emitAll(crowdloanListFlow(chain, accountId))
        }
    }

    private fun groupCrowdloans(crowdloans: List<Crowdloan>): GroupedCrowdloans {
        return crowdloans.groupBy { it.state::class }
            .toSortedMap(Crowdloan.State.STATE_CLASS_COMPARATOR)
    }

    private suspend fun crowdloanListFlow(
        chain: Chain,
        contributor: AccountId?,
    ): Flow<List<Crowdloan>> {
        // Crowdloans are no longer accessible and are deprecated. We will remove entire crowdloan feature soon
        return flowOf(emptyList())
    }
}
