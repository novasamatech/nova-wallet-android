package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import android.os.Parcelable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import kotlinx.parcelize.Parcelize
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPlan
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitSlice

@Parcelize
data class BittensorExitSlicePayload(val hotkey: String, val amount: String, val fullExit: Boolean) : Parcelable

@Parcelize
data class BittensorExitPositionPayload(val metaId: Long, val netuid: Int, val hotkeyAddress: String) : Parcelable {

    init {
        require(hotkeyAddress.isNotBlank())
    }

    fun netuid(): BittensorNetuid = BittensorNetuid.from(netuid)
}

@Parcelize
data class BittensorExitReviewPayload(
    val position: BittensorExitPositionPayload,
    val amount: String,
    val fullExit: Boolean,
    val estimatedTaoOut: String,
    val minimumTaoOut: String,
    val serviceFee: String,
    val limitPrice: String?,
    val exitHotkeys: List<String> = emptyList(),
    val slices: List<BittensorExitSlicePayload> = emptyList(),
    val spotPrice: String? = null,
    val simulatedInput: String? = null,
    val slippageFraction: String = "0.005",
    val groupHotkeys: List<String> = emptyList(),
    val serviceFeeRate: String? = null,
    val remainderSweepAccepted: Boolean = false,
    val costBasis: BittensorSaleCostBasisPayload? = null
) : Parcelable {

    fun quote() = BittensorExitQuote(
        position.netuid(),
        amount.toBigInteger(),
        fullExit,
        estimatedTaoOut.toBigInteger(),
        minimumTaoOut.toBigInteger(),
        serviceFee.toBigInteger(),
        limitPrice?.toBigInteger(),
        exitHotkeys.map { AccountIdKey(it.fromHex()) },
        slices.takeIf { it.isNotEmpty() }?.let { payloads ->
            BittensorExitPlan(payloads.map { BittensorExitSlice(AccountIdKey(it.hotkey.fromHex()), it.amount.toBigInteger(), it.fullExit) })
        },
        spotPrice?.toBigInteger(),
        simulatedInput?.toBigInteger(),
        slippageFraction.toBigDecimal(),
        groupHotkeys.map { AccountIdKey(it.fromHex()) }.toSet(),
        serviceFeeRate?.toBigDecimal()
    )
}

/** The setup's "Proceed" on a swept remainder covers the reviewed terms only; Max or a refreshed quote asks again. */
fun BittensorExitReviewPayload.carriesRemainderSweepFor(reviewed: BittensorExitQuote): Boolean =
    remainderSweepAccepted && reviewed == quote()

fun BittensorExitQuote.toPayload(position: BittensorExitPositionPayload) = BittensorExitReviewPayload(
    position,
    amount.toString(),
    fullExit,
    estimatedTaoOut.toString(),
    minimumTaoOut.toString(),
    serviceFee.toString(),
    limitPrice?.toString(),
    exitHotkeys.map { it.value.toHexString() },
    plan?.slices.orEmpty().map { BittensorExitSlicePayload(it.hotkey.value.toHexString(), it.amount.toString(), it.fullExit) },
    spotPrice?.toString(),
    simulatedInput?.toString(),
    slippageFraction.toPlainString(),
    groupHotkeys.map { it.value.toHexString() }.sorted(),
    serviceFeeRate?.toPlainString()
)
