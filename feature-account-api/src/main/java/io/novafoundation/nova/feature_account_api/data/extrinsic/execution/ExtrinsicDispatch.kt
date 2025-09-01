package io.novafoundation.nova.feature_account_api.data.extrinsic.execution

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

data class ExtrinsicExecutionResult(
    val extrinsicHash: String,
    val blockHash: BlockHash,
    val outcome: ExtrinsicDispatch,
    val submissionHierarchy: SubmissionHierarchy
)

sealed interface ExtrinsicDispatch {

    data class Ok(val emittedEvents: List<GenericEvent.Instance>) : ExtrinsicDispatch

    data class Failed(val error: DispatchError) : ExtrinsicDispatch

    object Unknown : ExtrinsicDispatch
}

fun ExtrinsicExecutionResult.requireOk(): ExtrinsicExecutionResult {
    return when (outcome) {
        is ExtrinsicDispatch.Failed -> throw outcome.error
        is ExtrinsicDispatch.Ok -> this
        ExtrinsicDispatch.Unknown -> throw IllegalArgumentException("Unknown extrinsic execution result")
    }
}

fun Result<ExtrinsicExecutionResult>.requireOk(): Result<ExtrinsicExecutionResult> {
    return mapCatching { it.requireOk() }
}

fun ExtrinsicExecutionResult.requireOutcomeOk(): ExtrinsicDispatch.Ok {
    return requireOk().outcome as ExtrinsicDispatch.Ok
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
