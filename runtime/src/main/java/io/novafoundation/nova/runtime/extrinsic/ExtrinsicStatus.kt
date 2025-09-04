package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.wsrpc.subscription.response.SubscriptionChange

sealed class ExtrinsicStatus(val extrinsicHash: String, val terminal: Boolean) {

    class Ready(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class Broadcast(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class InBlock(val blockHash: String, extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)

    class Finalized(val blockHash: String, extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = true)

    class Other(extrinsicHash: String) : ExtrinsicStatus(extrinsicHash, terminal = false)
}

private const val STATUS_READY = "ready"
private const val STATUS_BROADCAST = "broadcast"
private const val STATUS_IN_BLOCK = "inBlock"
private const val STATUS_FINALIZED = "finalized"
private const val STATUS_FINALITY_TIMEOUT = "finalityTimeout"

fun SubscriptionChange.asExtrinsicStatus(extrinsicHash: String): ExtrinsicStatus {
    return when (val result = params.result) {
        STATUS_READY -> ExtrinsicStatus.Ready(extrinsicHash)
        is Map<*, *> -> when {
            STATUS_BROADCAST in result -> ExtrinsicStatus.Broadcast(extrinsicHash)
            STATUS_IN_BLOCK in result -> ExtrinsicStatus.InBlock(extractBlockHash(result, STATUS_IN_BLOCK), extrinsicHash)
            STATUS_FINALIZED in result -> ExtrinsicStatus.Finalized(extractBlockHash(result, STATUS_FINALIZED), extrinsicHash)
            STATUS_FINALITY_TIMEOUT in result -> ExtrinsicStatus.Finalized(extractBlockHash(result, STATUS_FINALITY_TIMEOUT), extrinsicHash)
            else -> ExtrinsicStatus.Other(extrinsicHash)
        }
        else -> ExtrinsicStatus.Other(extrinsicHash)
    }
}

private fun extractBlockHash(map: Map<*, *>, key: String): String {
    return map[key] as? String ?: unknownStructure()
}

private fun unknownStructure(): Nothing = throw IllegalArgumentException("Unknown extrinsic status structure")
