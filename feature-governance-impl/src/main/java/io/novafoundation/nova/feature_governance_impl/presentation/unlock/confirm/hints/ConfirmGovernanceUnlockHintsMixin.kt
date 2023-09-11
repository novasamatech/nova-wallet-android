package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.buildSpannable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockAffects.RemainsLockedInfo
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ConfirmGovernanceUnlockHintsMixinFactory(
    private val resourceManager: ResourceManager
) {

    fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        remainsLockedInfoFlow: Flow<RemainsLockedInfo?>
    ): HintsMixin {
        return ConfirmGovernanceUnlockHintsMixin(resourceManager, scope, assetFlow, remainsLockedInfoFlow)
    }
}

private class ConfirmGovernanceUnlockHintsMixin(
    private val resourceManager: ResourceManager,
    scope: CoroutineScope,
    assetFlow: Flow<Asset>,
    remainsLockedInfoFlow: Flow<RemainsLockedInfo?>
) : HintsMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val hintsFlow: Flow<List<CharSequence>> = remainsLockedInfoFlow.map { remainsLockedInfo ->
        if (remainsLockedInfo != null) {
            listOf(remainsLockedHint(assetFlow.first(), remainsLockedInfo))
        } else {
            emptyList()
        }
    }.shareInBackground()

    private fun remainsLockedHint(
        asset: Asset,
        remainsLockedInfo: RemainsLockedInfo
    ): CharSequence {
        val amountPart = mapAmountToAmountModel(remainsLockedInfo.amount, asset).token
        val lockedIdsPart = remainsLockedInfo.lockedInIds.joinToString { lockId ->
            mapBalanceIdToUi(resourceManager, lockId)
        }

        return buildSpannable(resourceManager) {
            appendColored(amountPart, R.color.text_primary)

            append(" ")

            val rest = resourceManager.getString(R.string.referendum_unlock_remains_locked_format, lockedIdsPart)
            appendColored(rest, R.color.text_tertiary)
        }
    }
}
