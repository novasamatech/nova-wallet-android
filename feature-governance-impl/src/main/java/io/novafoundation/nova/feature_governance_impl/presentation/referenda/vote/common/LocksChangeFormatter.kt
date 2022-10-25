package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.model.AmountChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.model.LocksChangeModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlin.time.Duration

interface LocksChangeFormatter {
    suspend fun mapLocksChangeToUi(
        locksChange: GovernanceVoteAssistant.LocksChange,
        asset: Asset,
    ): LocksChangeModel
}

class RealLocksChangeFormatter(
    private val resourceManager: ResourceManager,
): LocksChangeFormatter {

    override suspend fun mapLocksChangeToUi(
        locksChange: GovernanceVoteAssistant.LocksChange,
        asset: Asset
    ): LocksChangeModel {
        return LocksChangeModel(
            amountChange = mapAmountChangeToUi(locksChange.lockedAmountChange, asset),
            periodChange = mapPeriodChangeToUi(locksChange.lockedPeriodChange),
            transferableChange = mapAmountChangeToUi(locksChange.transferableChange, asset)
        )
    }

    private fun mapAmountChangeToUi(
        lockedChange: GovernanceVoteAssistant.Change<Balance>,
        asset: Asset
    ): AmountChangeModel {
        val fromFormatted = mapAmountToAmountModel(lockedChange.previousValue, asset, includeAssetTicker = false).token
        val toFormatted = mapAmountToAmountModel(lockedChange.newValue, asset).token

        return when (lockedChange) {
            is GovernanceVoteAssistant.Change.Changed -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = mapAmountToAmountModel(lockedChange.absoluteDifference, asset).token,
                positive = lockedChange.positive
            )
            is GovernanceVoteAssistant.Change.Same -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = null,
                positive = null
            )
        }
    }

    private fun mapPeriodChangeToUi(periodChange: GovernanceVoteAssistant.Change<Duration>): AmountChangeModel {
        val from = resourceManager.formatDuration(periodChange.previousValue, estimated = false)
        val to = resourceManager.formatDuration(periodChange.newValue, estimated = false)

        return when (periodChange) {
            is GovernanceVoteAssistant.Change.Changed -> {
                val difference = resourceManager.formatDuration(periodChange.absoluteDifference, estimated = false)

                AmountChangeModel(
                    from = from,
                    to = to,
                    difference = resourceManager.getString(R.string.common_maximum_format, difference),
                    positive = periodChange.positive
                )
            }
            is GovernanceVoteAssistant.Change.Same -> AmountChangeModel(
                from = from,
                to = to,
                difference = null,
                positive = null
            )
        }
    }
}
