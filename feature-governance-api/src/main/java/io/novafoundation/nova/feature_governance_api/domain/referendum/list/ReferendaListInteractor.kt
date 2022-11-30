package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun referendaListStateFlow(voterAccountId: AccountId?, selectedGovernanceOption: SupportedGovernanceOption): Flow<ReferendaListState>
}
