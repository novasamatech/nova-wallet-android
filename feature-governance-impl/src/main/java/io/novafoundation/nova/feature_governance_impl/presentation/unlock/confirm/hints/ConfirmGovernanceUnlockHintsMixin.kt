package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.buildSpannable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockAffects.RemainsLockedInfo
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ConfirmGovernanceUnlockHintsMixinFactory(
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) {

    fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        remainsLockedInfoFlow: Flow<RemainsLockedInfo?>
    ): HintsMixin {
        return ConfirmGovernanceUnlockHintsMixin(resourceManager, scope, assetFlow, remainsLockedInfoFlow, amountFormatter)
    }
}

private class ConfirmGovernanceUnlockHintsMixin(
    private val resourceManager: ResourceManager,
    scope: CoroutineScope,
    assetFlow: Flow<Asset>,
    remainsLockedInfoFlow: Flow<RemainsLockedInfo?>,
    private val amountFormatter: AmountFormatter
) : HintsMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val hintsFlow: Flow<List<CharSequence>> = remainsLockedInfoFlow.map { remainsLockedInfo ->
        if (remainsLockedInfo != null && remainsLockedInfo.lockedInIds.isNotEmpty()) {
            listOf(remainsLockedHint(assetFlow.first(), remainsLockedInfo))
        } else {
            emptyList()
        }
    }.shareInBackground()

    private fun remainsLockedHint(
        asset: Asset,
        remainsLockedInfo: RemainsLockedInfo
    ): CharSequence {
        val amountPart = amountFormatter.formatAmountToAmountModel(remainsLockedInfo.amount, asset).token
        val lockedIdsPart = remainsLockedInfo.lockedInIds.joinToString { lockId ->
            mapBalanceIdToUi(resourceManager, lockId.value)
        }

        return buildSpannable(resourceManager) {
            appendColored(amountPart, R.color.text_primary)

            append(" ")

            val rest = resourceManager.getString(R.string.referendum_unlock_remains_locked_format, lockedIdsPart)
            appendColored(rest, R.color.text_secondary)
        }
    }
}
