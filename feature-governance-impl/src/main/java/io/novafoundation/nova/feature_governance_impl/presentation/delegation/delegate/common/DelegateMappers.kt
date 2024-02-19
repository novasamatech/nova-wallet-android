package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common

import androidx.annotation.StringRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.getConvictionVote
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.DelegatorVote
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateLabelModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateListModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateStatsModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.RecentVotes
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.formatConvictionVote
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

interface DelegateMappers {

    suspend fun mapDelegatePreviewToUi(delegatePreview: DelegatePreview, chainWithAsset: ChainWithAsset): DelegateListModel

    suspend fun formatDelegationsOverview(votes: Delegator.Vote, chainAsset: Chain.Asset): VoteModel

    suspend fun formatDelegation(delegation: Voting.Delegating, chainAsset: Chain.Asset): VoteModel

    suspend fun formatTrackDelegation(delegation: Voting.Delegating, track: Track, chainAsset: Chain.Asset): TrackDelegationModel

    fun mapDelegateTypeToUi(delegateType: DelegateAccountType?): DelegateTypeModel?

    suspend fun mapDelegateIconToUi(accountId: AccountId, metadata: Delegate.Metadata?): DelegateIcon

    suspend fun formatDelegateName(metadata: Delegate.Metadata?, identityName: String?, accountId: AccountId, chain: Chain): String

    suspend fun formatDelegationStats(stats: DelegatePreview.Stats, chainAsset: Chain.Asset): DelegateStatsModel

    fun formattedRecentVotesPeriod(@StringRes stringRes: Int): String

    suspend fun formatDelegateLabel(
        accountId: AccountId,
        metadata: Delegate.Metadata?,
        identityName: String?,
        chain: Chain
    ): DelegateLabelModel
}

suspend fun DelegateMappers.formatDelegationsOverviewOrNull(votes: Delegator.Vote?, chainAsset: Chain.Asset): VoteModel? {
    return votes?.let { formatDelegationsOverview(votes, chainAsset) }
}

class RealDelegateMappers(
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val trackFormatter: TrackFormatter,
    private val votersFormatter: VotersFormatter
) : DelegateMappers {

    override suspend fun mapDelegatePreviewToUi(
        delegatePreview: DelegatePreview,
        chainWithAsset: ChainWithAsset,
    ): DelegateListModel {
        return DelegateListModel(
            icon = mapDelegateIconToUi(delegatePreview.accountId, delegatePreview.metadata),
            accountId = delegatePreview.accountId,
            name = formatDelegateName(
                metadata = delegatePreview.metadata,
                identityName = delegatePreview.onChainIdentity?.display,
                accountId = delegatePreview.accountId,
                chain = chainWithAsset.chain
            ),
            type = mapDelegateTypeToUi(delegatePreview.metadata?.accountType),
            description = delegatePreview.metadata?.shortDescription,
            stats = delegatePreview.stats?.let { formatDelegationStats(it, chainWithAsset.asset) },
            delegation = delegatePreview.userDelegations?.let { mapDelegation(it, chainWithAsset.asset) }
        )
    }

    override suspend fun formatDelegationsOverview(votes: Delegator.Vote, chainAsset: Chain.Asset): VoteModel {
        val voteDetails = when (votes) {
            is Delegator.Vote.MultiTrack -> {
                resourceManager.getString(R.string.delegation_multi_track_format, votes.trackCount)
            }
            is Delegator.Vote.SingleTrack -> {
                votersFormatter.formatConvictionVoteDetails(votes.delegation, chainAsset)
            }
        }

        val totalVotes = votersFormatter.formatTotalVotes(votes)

        return VoteModel(totalVotes, voteDetails)
    }

    override suspend fun formatDelegation(delegation: Voting.Delegating, chainAsset: Chain.Asset): VoteModel {
        val convictionVote = delegation.getConvictionVote(chainAsset)

        return votersFormatter.formatConvictionVote(convictionVote, chainAsset)
    }

    override suspend fun formatTrackDelegation(delegation: Voting.Delegating, track: Track, chainAsset: Chain.Asset): TrackDelegationModel {
        return TrackDelegationModel(
            track = trackFormatter.formatTrack(track, chainAsset),
            delegation = formatDelegation(delegation, chainAsset)
        )
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

    override suspend fun mapDelegateIconToUi(accountId: AccountId, metadata: Delegate.Metadata?): DelegateIcon {
        val iconUrl = metadata?.iconUrl
        val accountType = metadata?.accountType

        return if (iconUrl != null) {
            val icon = Icon.FromLink(iconUrl)

            DelegateIcon(accountType.iconShape(), icon)
        } else {
            val addressIcon = addressIconGenerator.createAddressIcon(
                accountId,
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

    override suspend fun formatDelegateName(metadata: Delegate.Metadata?, identityName: String?, accountId: AccountId, chain: Chain): String {
        return identityName ?: metadata?.name ?: chain.addressOf(accountId)
    }

    override suspend fun formatDelegationStats(stats: DelegatePreview.Stats, chainAsset: Chain.Asset): DelegateStatsModel {
        return DelegateStatsModel(
            delegations = stats.delegationsCount.format(),
            delegatedVotes = chainAsset.amountFromPlanks(stats.delegatedVotes).format(),
            recentVotes = RecentVotes(
                label = formattedRecentVotesPeriod(R.string.delegation_recent_votes_format),
                value = stats.recentVotes.format()
            )
        )
    }

    override fun formattedRecentVotesPeriod(@StringRes stringRes: Int): String {
        return resourceManager.getString(
            stringRes,
            resourceManager.formatDuration(RECENT_VOTES_PERIOD, estimated = false)
        )
    }

    override suspend fun formatDelegateLabel(
        accountId: AccountId,
        metadata: Delegate.Metadata?,
        identityName: String?,
        chain: Chain
    ): DelegateLabelModel {
        return DelegateLabelModel(
            icon = mapDelegateIconToUi(accountId, metadata),
            address = chain.addressOf(accountId),
            name = formatDelegateName(
                metadata = metadata,
                identityName = identityName,
                accountId = accountId,
                chain = chain
            ),
            type = mapDelegateTypeToUi(metadata?.accountType)
        )
    }

    private suspend fun mapDelegation(votes: Map<Track, Voting.Delegating>, chainAsset: Chain.Asset): DelegateListModel.YourDelegationInfo? {
        if (votes.isEmpty()) return null

        val firstTrack = trackFormatter.formatTrack(votes.keys.first(), chainAsset)
        val otherTracksCount = votes.size - 1
        val otherTracksCountStr = if (otherTracksCount > 0) resourceManager.getString(R.string.delegate_more_tracks, otherTracksCount) else null

        val delegatorVotes = DelegatorVote(votes.values, chainAsset)

        return DelegateListModel.YourDelegationInfo(
            firstTrack = firstTrack,
            otherTracksCount = otherTracksCountStr,
            votes = formatDelegationsOverviewOrNull(delegatorVotes, chainAsset)
        )
    }
}
