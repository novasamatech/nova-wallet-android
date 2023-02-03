package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.isAllEquals
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateListModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateStatsModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.RecentVotes
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface DelegateMappers {

    suspend fun mapDelegatePreviewToUi(delegatePreview: DelegatePreview, chainWithAsset: ChainWithAsset): DelegateListModel

    fun mapDelegation(votes: Map<Track, Voting.Delegating>, chainAsset: Chain.Asset): DelegateListModel.YourDelegationInfo?

    fun mapDelegateTypeToUi(delegateType: DelegateAccountType?): DelegateTypeModel?

    fun mapVote(votes: List<Voting.Delegating>, chainAsset: Chain.Asset): DelegateListModel.Votes?

    suspend fun mapDelegateIconToUi(delegate: Delegate): DelegateIcon

    suspend fun formatDelegateName(delegate: Delegate, chain: Chain): String

    suspend fun formatDelegationStats(stats: DelegatePreview.Stats, chainAsset: Chain.Asset): DelegateStatsModel

    suspend fun formattedRecentVotesPeriod(): String
}

class RealDelegateMappers(
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val trackFormatter: TrackFormatter
) : DelegateMappers {

    override suspend fun mapDelegatePreviewToUi(
        delegatePreview: DelegatePreview,
        chainWithAsset: ChainWithAsset,
    ): DelegateListModel {
        return DelegateListModel(
            icon = mapDelegateIconToUi(delegatePreview),
            accountId = delegatePreview.accountId,
            name = formatDelegateName(delegatePreview, chainWithAsset.chain),
            type = mapDelegateTypeToUi(delegatePreview.metadata?.accountType),
            description = delegatePreview.metadata?.shortDescription,
            stats = formatDelegationStats(delegatePreview.stats, chainWithAsset.asset),
            delegation = delegatePreview.votes?.let { mapDelegation(it, chainWithAsset.asset) }
        )
    }

    override fun mapDelegation(votes: Map<Track, Voting.Delegating>, chainAsset: Chain.Asset): DelegateListModel.YourDelegationInfo? {
        if (votes.isEmpty()) return null

        val firstTrack = trackFormatter.formatTrack(votes.keys.first(), chainAsset)
        val otherTracksCount = votes.size - 1
        val otherTracksCountStr = if (otherTracksCount > 0) resourceManager.getString(R.string.delegate_more_tracks, otherTracksCount) else null
        return DelegateListModel.YourDelegationInfo(
            firstTrack = firstTrack,
            otherTracksCount = otherTracksCountStr,
            votes = mapVote(votes.values.toList(), chainAsset)
        )
    }

    override fun mapVote(votes: List<Voting.Delegating>, chainAsset: Chain.Asset): DelegateListModel.Votes? {
        val isAllVotesEquals = votes.isAllEquals { it.amount to it.conviction }

        if (isAllVotesEquals) {
            val firstVote = votes.first()
            val amount = chainAsset.amountFromPlanks(firstVote.amount)
            val preConvictionAmountFormatted = amount.formatTokenAmount(chainAsset)
            val multiplierFormatted = firstVote.conviction.amountMultiplier().format()

            val details = resourceManager.getString(
                R.string.referendum_voter_vote_details,
                preConvictionAmountFormatted,
                multiplierFormatted
            )
            return DelegateListModel.Votes(resourceManager.getString(R.string.referendum_voter_vote, amount.format()), details)
        }

        return null
    }

    override fun mapDelegateTypeToUi(delegateType: DelegateAccountType?): DelegateTypeModel? {
        return when (delegateType) {
            DelegateAccountType.INDIVIDUAL -> DelegateTypeModel(
                text = resourceManager.getString(R.string.delegation_delegate_type_individual),
                iconRes = R.drawable.ic_individual,
                textColorRes = R.color.individual_chip_text,
                backgroundColorRes = R.color.individual_chip_background,
                iconColorRes = R.color.individual_chip_icon,
            )

            DelegateAccountType.ORGANIZATION -> DelegateTypeModel(
                text = resourceManager.getString(R.string.delegation_delegate_type_organization),
                iconRes = R.drawable.ic_organization,
                iconColorRes = R.color.organization_chip_icon,
                textColorRes = R.color.organization_chip_icon,
                backgroundColorRes = R.color.organization_chip_background,
            )

            null -> null
        }
    }

    override suspend fun mapDelegateIconToUi(delegate: Delegate): DelegateIcon {
        val iconUrl = delegate.metadata?.iconUrl
        val accountType = delegate.metadata?.accountType

        return if (iconUrl != null) {
            val icon = Icon.FromLink(iconUrl)

            DelegateIcon(accountType.iconShape(), icon)
        } else {
            val addressIcon = addressIconGenerator.createAddressIcon(
                delegate.accountId,
                AddressIconGenerator.SIZE_BIG,
                AddressIconGenerator.BACKGROUND_TRANSPARENT
            )
            val icon = Icon.FromDrawable(addressIcon)

            DelegateIcon(DelegateIcon.IconShape.NONE, icon)
        }
    }

    private fun DelegateAccountType?.iconShape(): DelegateIcon.IconShape {
        return when (this) {
            DelegateAccountType.INDIVIDUAL -> DelegateIcon.IconShape.ROUND
            DelegateAccountType.ORGANIZATION -> DelegateIcon.IconShape.SQUARE
            null -> DelegateIcon.IconShape.NONE
        }
    }

    override suspend fun formatDelegateName(delegate: Delegate, chain: Chain): String {
        val metadataName = delegate.metadata?.name
        val identityName = delegate.onChainIdentity?.display

        return when {
            identityName != null -> identityName
            metadataName != null -> metadataName
            else -> chain.addressOf(delegate.accountId)
        }
    }

    override suspend fun formatDelegationStats(stats: DelegatePreview.Stats, chainAsset: Chain.Asset): DelegateStatsModel {
        return DelegateStatsModel(
            delegations = stats.delegationsCount.format(),
            delegatedVotes = chainAsset.amountFromPlanks(stats.delegatedVotes).format(),
            recentVotes = RecentVotes(
                label = formattedRecentVotesPeriod(),
                value = stats.recentVotes.format()
            )
        )
    }

    override suspend fun formattedRecentVotesPeriod(): String {
        return resourceManager.getString(
            R.string.delegation_recent_votes_format,
            resourceManager.formatDuration(RECENT_VOTES_PERIOD, estimated = false)
        )
    }
}
