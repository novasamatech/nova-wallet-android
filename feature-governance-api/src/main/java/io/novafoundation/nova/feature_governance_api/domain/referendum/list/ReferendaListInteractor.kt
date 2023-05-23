package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun searchReferendaListStateFlow(
        queryFlow: Flow<String>,
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope
    ): Flow<List<ReferendumPreview>>

    fun referendaListStateFlow(
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope,
        referendumTypeFilterFlow: Flow<ReferendumTypeFilter>
    ): Flow<ReferendaListState>

    fun votedReferendaListFlow(
        voter: Voter,
        onlyRecentVotes: Boolean
    ): Flow<List<ReferendumPreview>>
}

class Voter(val accountId: AccountId, val type: Type) {

    companion object;

    enum class Type {
        USER, ACCOUNT
    }
}

fun Voter.Companion.user(accountId: AccountId) = Voter(accountId, Voter.Type.USER)

fun Voter.Companion.account(accountId: AccountId) = Voter(accountId, Voter.Type.ACCOUNT)
