package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TreasuryProposal
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParser
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class TreasuryApproveProposalParser(
    private val treasuryRepository: TreasuryRepository
) : ReferendumCallParser {

    override suspend fun parse(preImage: PreImage, chainId: ChainId): ReferendumCall? = runCatching {
        val call = preImage.call

        if (!call.instanceOf(Modules.TREASURY, "approve_proposal")) return null

        val id = bindNumber(call.arguments["proposal_id"])

        val proposal = treasuryRepository.getTreasuryProposal(chainId, TreasuryProposal.Id(id)) ?: return null

        ReferendumCall.TreasuryRequest(amount = proposal.amount, beneficiary = proposal.beneficiary)
    }.getOrNull()
}
