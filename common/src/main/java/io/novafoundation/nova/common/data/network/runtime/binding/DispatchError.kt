package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.metadata
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.error
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.module.ErrorMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module as RuntimeModule

sealed class DispatchError : Throwable() {

    data class Module(val module: RuntimeModule, val error: ErrorMetadata) : DispatchError() {

        override val message: String
            get() = toString()

        override fun toString(): String {
            return "${module.name}.${error.name}"
        }
    }

    object Token : DispatchError() {

        override val message: String
            get() = toString()

        override fun toString(): String {
            return "Not enough tokens"
        }
    }

    object Unknown : DispatchError()
}

context(RuntimeContext)
fun bindDispatchError(decoded: Any?): DispatchError {
    val asDictEnum = decoded.castToDictEnum()

    return when (asDictEnum.name) {
        "Module" -> {
            val moduleErrorStruct = asDictEnum.value.castToStruct()

            val moduleIndex = bindInt(moduleErrorStruct["index"])
            val errorIndex = bindModuleError(moduleErrorStruct["error"])

            val module = metadata.module(moduleIndex)
            val error = module.error(errorIndex)

            DispatchError.Module(module, error)
        }

        "Token" -> DispatchError.Token

        else -> DispatchError.Unknown
    }
}

private fun bindModuleError(errorEncoded: ByteArray?): Int {
    requireNotNull(errorEncoded) {
        "Error should exist"
    }

    return errorEncoded[0].toInt()
}
