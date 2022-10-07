package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun referendaFlow(voterAccountId: AccountId): Flow<List<ReferendumPreview>>
}
