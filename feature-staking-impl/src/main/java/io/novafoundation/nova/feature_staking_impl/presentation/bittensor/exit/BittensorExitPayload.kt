package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorExitPositionPayload(val metaId: Long, val netuid: Int, val hotkeyAddress: String) : Parcelable {

    init { require(hotkeyAddress.isNotBlank()) }

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
    val limitPrice: String?
) : Parcelable {

    fun quote() = BittensorExitQuote(
        position.netuid(),
        amount.toBigInteger(),
        fullExit,
        estimatedTaoOut.toBigInteger(),
        minimumTaoOut.toBigInteger(),
        serviceFee.toBigInteger(),
        limitPrice?.toBigInteger()
    )
}

fun BittensorExitQuote.toPayload(position: BittensorExitPositionPayload) = BittensorExitReviewPayload(
    position,
    amount.toString(),
    fullExit,
    estimatedTaoOut.toString(),
    minimumTaoOut.toString(),
    serviceFee.toString(),
    limitPrice?.toString()
)
