package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun searchReferendaListStateFlow(
        metaAccount: MetaAccount,
        queryFlow: Flow<String>,
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope
    ): Flow<ExtendedLoadingState<List<ReferendumPreview>>>

    fun referendaListStateFlow(
        metaAccount: MetaAccount,
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope,
        referendumTypeFilterFlow: Flow<ReferendumTypeFilter>
    ): Flow<ExtendedLoadingState<ReferendaListState>>

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
