package io.novafoundation.nova.feature_account_api.data.extrinsic.execution

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

data class ExtrinsicExecutionResult(
    val extrinsicHash: String,
    val blockHash: BlockHash,
    val outcome: ExtrinsicDispatch
)

sealed interface ExtrinsicDispatch {

    data class Ok(val emittedEvents: List<GenericEvent.Instance>) : ExtrinsicDispatch

    data class Failed(val error: DispatchError) : ExtrinsicDispatch

    object Unknown : ExtrinsicDispatch
}

fun ExtrinsicExecutionResult.requireOk(): ExtrinsicDispatch.Ok {
    return when (outcome) {
        is ExtrinsicDispatch.Failed -> throw outcome.error
        is ExtrinsicDispatch.Ok -> outcome
        ExtrinsicDispatch.Unknown -> throw IllegalArgumentException("Unknown extrinsic execution result")
    }
}

fun Result<ExtrinsicExecutionResult>.requireOk(): Result<ExtrinsicDispatch.Ok> {
    return mapCatching {
        it.requireOk()
    }
}

fun ExtrinsicDispatch.isOk(): Boolean {
    return this is ExtrinsicDispatch.Ok
}

fun ExtrinsicDispatch.isModuleError(moduleName: String, errorName: String): Boolean {
    return this is ExtrinsicDispatch.Failed &&
        error is DispatchError.Module &&
        error.module.name == moduleName &&
        error.error.name == errorName
}
