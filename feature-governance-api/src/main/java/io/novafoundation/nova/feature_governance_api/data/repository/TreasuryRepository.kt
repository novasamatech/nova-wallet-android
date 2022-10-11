package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TreasuryProposal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface TreasuryRepository {

    suspend fun getTreasuryProposal(chainId: ChainId, id: TreasuryProposal.Id): TreasuryProposal?
}
