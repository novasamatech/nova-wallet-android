package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.wsrpc.subscription.response.SubscriptionChange

sealed class ExtrinsicStatus(val extrinsicHash: String, val terminal: Boolean) {

    class Ready(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class Broadcast(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class InBlock(val blockHash: String, extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class Finalized(val blockHash: String, extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = true)

    sealed class Failure(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = true)

    class Invalid(extrinsicHash: String) : Failure(extrinsicHash)

    class Dropped(extrinsicHash: String) : Failure(extrinsicHash)

    class Usurped(val replacementHash: String, extrinsicHash: String) : Failure(extrinsicHash)

    // The watch ended without finality evidence; this is not a dispatch failure or a finalized block.
    class FinalityTimeout(val blockHash: String, extrinsicHash: String) : Failure(extrinsicHash)

    class Other(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)
}

class ExtrinsicWatchException(val status: ExtrinsicStatus) : IllegalStateException(
    "Extrinsic watch ended with ${status.javaClass.simpleName} before the requested status"
)

private const val STATUS_READY = "ready"
private const val STATUS_INVALID = "invalid"
private const val STATUS_DROPPED = "dropped"
private const val STATUS_USURPED = "usurped"
private const val STATUS_BROADCAST = "broadcast"
private const val STATUS_IN_BLOCK = "inBlock"
private const val STATUS_FINALIZED = "finalized"
private const val STATUS_FINALITY_TIMEOUT = "finalityTimeout"

fun SubscriptionChange.asExtrinsicStatus(extrinsicHash: String): ExtrinsicStatus {
    return when (val result = params.result) {
        STATUS_READY -> ExtrinsicStatus.Ready(extrinsicHash)
        STATUS_INVALID -> ExtrinsicStatus.Invalid(extrinsicHash)
        STATUS_DROPPED -> ExtrinsicStatus.Dropped(extrinsicHash)
        is Map<*, *> -> when {
            STATUS_BROADCAST in result -> ExtrinsicStatus.Broadcast(extrinsicHash)
            STATUS_IN_BLOCK in result -> ExtrinsicStatus.InBlock(extractBlockHash(result, STATUS_IN_BLOCK), extrinsicHash)
            STATUS_FINALIZED in result -> ExtrinsicStatus.Finalized(extractBlockHash(result, STATUS_FINALIZED), extrinsicHash)
            STATUS_FINALITY_TIMEOUT in result -> ExtrinsicStatus.FinalityTimeout(extractBlockHash(result, STATUS_FINALITY_TIMEOUT), extrinsicHash)
            STATUS_USURPED in result -> ExtrinsicStatus.Usurped(extractBlockHash(result, STATUS_USURPED), extrinsicHash)
            else -> ExtrinsicStatus.Other(extrinsicHash)
        }

        else -> ExtrinsicStatus.Other(extrinsicHash)
    }
}

private fun extractBlockHash(map: Map<*, *>, key: String): String {
    return map[key] as? String ?: unknownStructure()
}

private fun unknownStructure(): Nothing = throw IllegalArgumentException("Unknown extrinsic status structure")
