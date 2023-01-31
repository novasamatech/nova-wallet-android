package io.novafoundation.nova.feature_governance_impl.presentation.voters

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter.ConvictionVote
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface VotersFormatter {

    suspend fun formatVoter(
        voter: GenericVoter<*>,
        chain: Chain,
        chainAsset: Chain.Asset,
        voteCountDetails: String,
    ): VoterModel

    suspend fun formatConvictionVote(
        convictionVote: ConvictionVote,
        chainAsset: Chain.Asset
    ): String
}

suspend fun VotersFormatter.formatConvictionVoter(
    voter: GenericVoter<ConvictionVote?>,
    chain: Chain,
    chainAsset: Chain.Asset,
): VoterModel {
    val formattedVote = voter.vote?.let { formatConvictionVote(it, chainAsset) }.orEmpty()
    return formatVoter(voter, chain, chainAsset, formattedVote)
}

class RealVotersFormatter(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
) : VotersFormatter {

    override suspend fun formatVoter(voter: GenericVoter<*>, chain: Chain, chainAsset: Chain.Asset, voteCountDetails: String): VoterModel {
        val addressModel = addressIconGenerator.createAccountAddressModel(chain, voter.accountId, voter.identity?.name)
        val total = voter.vote?.totalVotes?.format().orEmpty()

        return VoterModel(
            addressModel = addressModel,
            votesCount = resourceManager.getString(R.string.referendum_voter_vote, total),
            votesCountDetails = voteCountDetails
        )
    }

    override suspend fun formatConvictionVote(convictionVote: ConvictionVote, chainAsset: Chain.Asset): String {
        val preConvictionAmountFormatted = convictionVote.amount.formatTokenAmount(chainAsset)
        val multiplierFormatted = convictionVote.conviction.amountMultiplier().format()

        return resourceManager.getString(
            R.string.referendum_voter_vote_details,
            preConvictionAmountFormatted,
            multiplierFormatted
        )
    }
}
