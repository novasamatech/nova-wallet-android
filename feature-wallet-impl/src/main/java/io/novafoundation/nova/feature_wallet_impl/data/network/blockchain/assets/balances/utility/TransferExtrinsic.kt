package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.extrinsicHash
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import java.math.BigInteger

class TransferExtrinsic(
    val senderId: ByteArray,
    val recipientId: ByteArray,
    val amountInPlanks: BigInteger,
    val index: Pair<Int, Int>,
    val hash: String,
)

private val TRANSFER_CALL_NAMES = listOf("transfer", "transfer_keep_alive")

fun notTransfer(): Nothing = throw IllegalArgumentException("Extrinsic is not a transfer extrinsic")

fun bindTransferExtrinsic(scale: String, runtime: RuntimeSnapshot): TransferExtrinsic {
    val extrinsicInstance = Extrinsic.Default.fromHexOrIncompatible(scale, runtime)
    val call = extrinsicInstance.call

    val transferModule = runtime.metadata.balances()
    val transferCalls = TRANSFER_CALL_NAMES.map(transferModule::call)

    val isTransferCall = transferCalls.any { it.index == call.function.index }

    if (!isTransferCall) throw notTransfer()

    val senderId = bindAccountIdentifier(extrinsicInstance.signature!!.accountIdentifier)
    val recipientId = bindAccountIdentifier(call.arguments["dest"])

    return TransferExtrinsic(
        senderId = senderId,
        recipientId = recipientId,
        amountInPlanks = bindNumber(call.arguments["value"]),
        index = call.function.index,
        hash = scale.extrinsicHash()
    )
}
