package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TreasuryProposal
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParseContext
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class TreasuryApproveProposalAdapter(
    private val treasuryRepository: TreasuryRepository
) : ReferendumCallAdapter {

    override suspend fun fromCall(call: GenericCall.Instance, context: ReferendumCallParseContext): ReferendumCall? {
        if (!call.instanceOf(Modules.TREASURY, "approve_proposal")) return null

        val id = bindNumber(call.arguments["proposal_id"])

        val proposal = treasuryRepository.getTreasuryProposal(context.chainId, TreasuryProposal.Id(id)) ?: return null

        return ReferendumCall.TreasuryRequest(amount = proposal.amount, beneficiary = proposal.beneficiary)
    }
}
