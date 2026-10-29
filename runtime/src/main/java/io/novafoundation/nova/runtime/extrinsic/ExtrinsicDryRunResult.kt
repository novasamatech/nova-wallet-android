package io.novafoundation.nova.runtime.extrinsic

import io.emeraldpay.polkaj.scale.ScaleCodecReader
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.utils.provideContext
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed interface ExtrinsicDryRunResult {
    data object Success : ExtrinsicDryRunResult
    data class DispatchFailure(val description: String) : ExtrinsicDryRunResult
    data class InvalidTransaction(val description: String) : ExtrinsicDryRunResult
}

/** SCALE ApplyExtrinsicResult = Result<Result<(), DispatchError>, TransactionValidityError>. */
internal fun decodeExtrinsicDryRunResult(hex: String, runtime: RuntimeSnapshot): ExtrinsicDryRunResult {
    require(hex.matches(Regex("0x(?:[0-9a-fA-F]{2})+"))) { "Malformed dry-run response" }
    val reader = ScaleCodecReader(hex.fromHex())
    val result = when (reader.readUByte()) {
        0 -> when (reader.readUByte()) {
            0 -> ExtrinsicDryRunResult.Success
            1 -> {
                val type = requireNotNull(runtime.typeRegistry["DispatchError"]) { "Missing DispatchError type" }
                val decoded = type.decode(reader, runtime)
                val error = runtime.provideContext { bindDispatchError(decoded) }
                ExtrinsicDryRunResult.DispatchFailure(error.toString())
            }
            else -> error("Unrecognized dry-run dispatch result")
        }
        1 -> {
            val type = requireNotNull(runtime.typeRegistry["TransactionValidityError"]) { "Missing transaction validity type" }
            val decoded = type.decode(reader, runtime)
            ExtrinsicDryRunResult.InvalidTransaction(formatValidityError(decoded))
        }
        else -> error("Unrecognized dry-run response")
    }
    require(!reader.hasNext()) { "Trailing bytes in dry-run response" }
    return result
}

private fun formatValidityError(value: Any?): String = when (value) {
    is DictEnum.Entry<*> -> listOfNotNull(value.name, value.value?.let(::formatValidityError)).joinToString(".")
    else -> value.toString()
}
