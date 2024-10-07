package io.novafoundation.nova.feature_governance_impl.presentation.common.voters

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter.ConvictionVote
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigDecimal

interface VotersFormatter {

    suspend fun formatVoter(
        voter: GenericVoter<*>,
        chain: Chain,
        chainAsset: Chain.Asset,
        voteCountDetails: String,
    ): VoterModel

    suspend fun formatVoter(
        voter: GenericVoter<*>,
        chain: Chain,
        chainAsset: Chain.Asset,
        voteModel: VoteModel
    ): VoterModel

    fun formatConvictionVoteDetails(
        convictionVote: ConvictionVote,
        chainAsset: Chain.Asset
    ): String

    fun formatTotalVotes(vote: GenericVoter.Vote?): String

    fun formatVoteType(voteType: VoteType): VoteDirectionModel

    fun formatVotes(
        amount: BigDecimal,
        voteType: VoteType,
        conviction: Conviction
    ): String
}

suspend fun VotersFormatter.formatConvictionVote(convictionVote: ConvictionVote, chainAsset: Chain.Asset): VoteModel {
    return VoteModel(
        votesCount = formatTotalVotes(convictionVote),
        votesCountDetails = formatConvictionVoteDetails(convictionVote, chainAsset)
    )
}

suspend fun VotersFormatter.formatConvictionVoter(
    voter: GenericVoter<ConvictionVote?>,
    chain: Chain,
    chainAsset: Chain.Asset,
): VoterModel {
    val formattedVote = voter.vote?.let { formatConvictionVoteDetails(it, chainAsset) }.orEmpty()
    return formatVoter(voter, chain, chainAsset, formattedVote)
}

class RealVotersFormatter(
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager
) : VotersFormatter {

    override suspend fun formatVoter(voter: GenericVoter<*>, chain: Chain, chainAsset: Chain.Asset, voteCountDetails: String): VoterModel {
        return formatVoter(
            voter = voter,
            chain = chain,
            chainAsset = chainAsset,
            voteModel = VoteModel(
                votesCount = formatTotalVotes(voter.vote),
                votesCountDetails = voteCountDetails
            )
        )
    }

    override suspend fun formatVoter(voter: GenericVoter<*>, chain: Chain, chainAsset: Chain.Asset, voteModel: VoteModel): VoterModel {
        val addressModel = addressIconGenerator.createAccountAddressModel(chain, voter.accountId, voter.identity?.name)

        return VoterModel(
            addressModel = addressModel,
            vote = voteModel
        )
    }

    override fun formatConvictionVoteDetails(convictionVote: ConvictionVote, chainAsset: Chain.Asset): String {
        val preConvictionAmountFormatted = convictionVote.amount.formatTokenAmount(chainAsset)
        val multiplierFormatted = convictionVote.conviction.amountMultiplier().format()

        return resourceManager.getString(
            R.string.referendum_voter_vote_details,
            preConvictionAmountFormatted,
            multiplierFormatted
        )
    }

    override fun formatVotes(amount: BigDecimal, voteType: VoteType, conviction: Conviction): String {
        val votes = if (voteType == VoteType.ABSTAIN) {
            amount * Conviction.None.amountMultiplier()
        } else {
            amount * conviction.amountMultiplier()
        }

        return resourceManager.getString(R.string.referendum_voter_vote, votes.format())
    }

    override fun formatVoteType(voteType: VoteType): VoteDirectionModel {
        return when (voteType) {
            VoteType.AYE -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_aye), R.color.text_positive)
            VoteType.NAY -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_nay), R.color.text_negative)
            VoteType.ABSTAIN -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_abstain), R.color.text_secondary)
        }
    }

    override fun formatTotalVotes(vote: GenericVoter.Vote?): String {
        val formattedAmount = vote?.totalVotes.orZero().format()

        return resourceManager.getString(R.string.referendum_voter_vote, formattedAmount)
    }
}
