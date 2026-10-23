package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import java.math.BigInteger

/** One row of the "choose a validator" sheet (iOS `SubtensorValidatorPickerViewModelFactory`). */
data class BittensorPositionChoice(
    val position: BittensorExitPositionPayload,
    val hotkey: AccountIdKey,
    /** The validator name, or its full address when it has none (the row shortens it in the middle). */
    val title: String,
    /** The stake held with this validator, already formatted in the subnet token. */
    val staked: String
)

class BittensorPositionChoiceUi(val choice: BittensorPositionChoice, val icon: Drawable)

data class BittensorPositionChoiceRequest(val action: BittensorPositionTradeAction, val choices: List<BittensorPositionChoiceUi>)

/** Keeps the [held] order: the primary (largest) position first, as on the position screen. */
internal fun bittensorPositionChoices(
    held: List<BittensorSubnetPosition>,
    metaId: Long,
    names: Map<AccountIdKey, String>,
    addressOf: (ByteArray) -> String,
    formatStaked: (BigInteger) -> CharSequence
): List<BittensorPositionChoice> = held.map { position ->
    val address = addressOf(position.validatorHotkey.value)
    BittensorPositionChoice(
        position = BittensorExitPositionPayload(metaId, position.subnetId.value, address),
        hotkey = position.validatorHotkey,
        title = names[position.validatorHotkey]?.takeIf { it.isNotBlank() } ?: address,
        staked = formatStaked(position.alphaInPlanks).toString().ltrIsolated()
    )
}
