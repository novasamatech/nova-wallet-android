package io.novafoundation.nova.feature_governance_api.domain.tindergov

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface TinderGovInteractor {

    fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>>

    suspend fun loadReferendumSummary(id: ReferendumId): String?

    suspend fun loadReferendumAmount(referendumPreview: ReferendumPreview): BigInteger?
}
