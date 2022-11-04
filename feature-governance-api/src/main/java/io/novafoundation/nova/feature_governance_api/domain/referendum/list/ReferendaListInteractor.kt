package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun referendaListStateFlow(voterAccountId: AccountId?, chain: Chain, chainAsset: Chain.Asset): Flow<ReferendaListState>
}
