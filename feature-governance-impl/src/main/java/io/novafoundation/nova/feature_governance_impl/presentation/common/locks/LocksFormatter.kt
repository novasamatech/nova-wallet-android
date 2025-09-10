package io.novafoundation.nova.feature_governance_impl.presentation.common.locks

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.toAmountInput
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel

interface LocksFormatter {

    fun formatReusableLock(reusableLock: ReusableLock, asset: Asset): AmountChipModel
}

class RealLocksFormatter(
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) : LocksFormatter {

    override fun formatReusableLock(
        reusableLock: ReusableLock,
        asset: Asset
    ): AmountChipModel {
        val labelFormat = when (reusableLock.type) {
            ReusableLock.Type.GOVERNANCE -> R.string.referendum_vote_chip_governance_lock
            ReusableLock.Type.ALL -> R.string.referendum_vote_chip_all_locks
        }

        val amount = asset.token.amountFromPlanks(reusableLock.amount)
        val amountModel = amountFormatter.formatAmountToAmountModel(amount, asset)

        return AmountChipModel(
            amountInput = amount.toAmountInput(),
            label = resourceManager.getString(labelFormat, amountModel.token)
        )
    }
}
