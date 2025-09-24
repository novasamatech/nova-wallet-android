package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.AmountChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.LocksChangeModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import kotlin.time.Duration

interface LocksChangeFormatter {

    suspend fun mapLocksChangeToUi(
        locksChange: LocksChange,
        asset: Asset,
        displayPeriodFromWhenSame: Boolean = true,
    ): LocksChangeModel

    suspend fun mapAmountChangeToUi(
        lockedChange: Change<Balance>,
        asset: Asset
    ): AmountChangeModel
}

class RealLocksChangeFormatter(
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) : LocksChangeFormatter {

    override suspend fun mapLocksChangeToUi(
        locksChange: LocksChange,
        asset: Asset,
        displayPeriodFromWhenSame: Boolean,
    ): LocksChangeModel {
        return LocksChangeModel(
            amountChange = mapAmountChangeToUi(locksChange.lockedAmountChange, asset),
            periodChange = mapPeriodChangeToUi(locksChange.lockedPeriodChange, displayPeriodFromWhenSame),
            transferableChange = mapAmountChangeToUi(locksChange.transferableChange, asset)
        )
    }

    override suspend fun mapAmountChangeToUi(
        lockedChange: Change<Balance>,
        asset: Asset
    ): AmountChangeModel {
        val fromFormatted = amountFormatter.formatAmountToAmountModel(lockedChange.previousValue, asset, AmountConfig(includeAssetTicker = false)).token
        val toFormatted = amountFormatter.formatAmountToAmountModel(lockedChange.newValue, asset).token

        return when (lockedChange) {
            is Change.Changed -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = amountFormatter.formatAmountToAmountModel(lockedChange.absoluteDifference, asset).token,
                positive = lockedChange.positive
            )
            is Change.Same -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = null,
                positive = null
            )
        }
    }

    private fun mapPeriodChangeToUi(periodChange: Change<Duration>, displayPeriodFromWhenSame: Boolean): AmountChangeModel {
        val from = resourceManager.formatDuration(periodChange.previousValue, estimated = false)
        val to = resourceManager.formatDuration(periodChange.newValue, estimated = false)

        return when (periodChange) {
            is Change.Changed -> {
                val difference = resourceManager.formatDuration(periodChange.absoluteDifference, estimated = false)

                AmountChangeModel(
                    from = from,
                    to = to,
                    difference = resourceManager.getString(R.string.common_maximum_format, difference),
                    positive = periodChange.positive
                )
            }
            is Change.Same -> AmountChangeModel(
                from = from.takeIf { displayPeriodFromWhenSame },
                to = to,
                difference = null,
                positive = null
            )
        }
    }
}
