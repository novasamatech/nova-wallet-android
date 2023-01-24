package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun referendaListStateFlow(
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption
    ): Flow<ReferendaListState>

    fun referendaListFlow(
        voter: Voter,
        onlyVoted: Boolean
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
