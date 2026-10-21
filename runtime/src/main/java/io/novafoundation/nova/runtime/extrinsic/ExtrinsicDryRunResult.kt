package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.utils.provideContext
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed interface ExtrinsicDryRunResult {
    data object Success : ExtrinsicDryRunResult
    data class DispatchFailure(val description: String) : ExtrinsicDryRunResult
    data class InvalidTransaction(val description: String) : ExtrinsicDryRunResult
}

/** SCALE ApplyExtrinsicResult = Result<Result<(), DispatchError>, TransactionValidityError>. */
internal fun decodeExtrinsicDryRunResult(hex: String, runtime: RuntimeSnapshot): ExtrinsicDryRunResult {
    require(hex.matches(Regex("0x(?:[0-9a-fA-F]{2})+"))) { "Malformed dry-run response" }
    return when {
        hex == "0x0000" -> ExtrinsicDryRunResult.Success
        hex.startsWith("0x0001") -> {
            val type = requireNotNull(runtime.typeRegistry["DispatchError"]) { "Missing DispatchError type" }
            val decoded = type.fromHex(runtime, "0x" + hex.drop(6))
            val error = runtime.provideContext { bindDispatchError(decoded) }
            ExtrinsicDryRunResult.DispatchFailure(error.toString())
        }
        hex.startsWith("0x01") -> {
            val type = requireNotNull(runtime.typeRegistry["TransactionValidityError"]) { "Missing transaction validity type" }
            val decoded = type.fromHex(runtime, "0x" + hex.drop(4))
            ExtrinsicDryRunResult.InvalidTransaction(formatValidityError(decoded))
        }
        else -> error("Unrecognized dry-run response")
    }
}

private fun formatValidityError(value: Any?): String = when (value) {
    is DictEnum.Entry<*> -> listOfNotNull(value.name, value.value?.let(::formatValidityError)).joinToString(".")
    else -> value.toString()
}
