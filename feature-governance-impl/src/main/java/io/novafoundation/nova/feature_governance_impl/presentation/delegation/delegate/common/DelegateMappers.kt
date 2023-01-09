package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateStats
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateStatsModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel.IconShape
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface DelegateMappers {

    fun mapDelegateTypeToUi(delegateType: DelegateAccountType?): DelegateTypeModel?

    suspend fun mapDelegateIconToUi(delegate: Delegate): Icon

    suspend fun formatDelegateName(delegate: Delegate, chain: Chain): String

    suspend fun formatDelegationStats(stats: DelegateStats, chainAsset: Chain.Asset): DelegateStatsModel
}

class RealDelegateMappers(
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
) : DelegateMappers {

    override fun mapDelegateTypeToUi(delegateType: DelegateAccountType?): DelegateTypeModel? {
        return when (delegateType) {
            DelegateAccountType.INDIVIDUAL -> DelegateTypeModel(
                text = resourceManager.getString(R.string.delegation_delegate_type_individual),
                iconRes = R.drawable.ic_individual,
                textColorRes = R.color.individual_chip_text,
                backgroundColorRes = R.color.individual_chip_background,
                iconColorRes = R.color.individual_chip_icon,
                delegateIconShape = IconShape.ROUND
            )

            DelegateAccountType.ORGANIZATION -> DelegateTypeModel(
                text = resourceManager.getString(R.string.delegation_delegate_type_individual),
                iconRes = R.drawable.ic_organization,
                iconColorRes = R.color.organization_chip_icon,
                textColorRes = R.color.organization_chip_icon,
                backgroundColorRes = R.color.organization_chip_background,
                delegateIconShape = IconShape.SQUARE
            )

            null -> null
        }
    }

    override suspend fun mapDelegateIconToUi(delegate: Delegate): Icon {
        val iconUrl = delegate.metadata?.iconUrl

        return if (iconUrl != null) {
            Icon.FromLink(iconUrl)
        } else {
            val addressIcon = addressIconGenerator.createAddressIcon(
                delegate.accountId,
                AddressIconGenerator.SIZE_BIG,
                AddressIconGenerator.BACKGROUND_TRANSPARENT
            )

            Icon.FromDrawable(addressIcon)
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

    override suspend fun formatDelegationStats(stats: DelegateStats, chainAsset: Chain.Asset): DelegateStatsModel {
        return DelegateStatsModel(
            delegations = stats.delegationsCount.format(),
            delegatedVotes = chainAsset.amountFromPlanks(stats.delegatedVotes).format(),
            recentVotes = DelegateStatsModel.RecentVotes(
                label = resourceManager.getString(
                    R.string.delegation_recent_votes_format,
                    resourceManager.formatDuration(stats.recentVotes.period, estimated = false)
                ),
                value = stats.recentVotes.numberOfVotes.format()
            )
        )
    }
}
