package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface ReferendaListInteractor {

    fun referendaFlow(metaAccount: MetaAccount, chain: Chain): Flow<GroupedList<ReferendumGroup, ReferendumPreview>>
}
