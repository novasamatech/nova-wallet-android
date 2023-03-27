package io.novafoundation.nova.runtime.ethereum

class EvmRpcException(val type: Type, message: String) : Throwable("${type.name}: $message") {

    enum class Type(val code: Int?) {
        EXECUTION_REVERTED(-32603),
        INVALID_INPUT(-32000),
        UNKNOWN(null);

        companion object {
            fun fromCode(code: Int): Type {
                return values().firstOrNull { it.code == code } ?: UNKNOWN
            }
        }
    }
}

fun EvmRpcException(code: Int, message: String): EvmRpcException {
    return EvmRpcException(EvmRpcException.Type.fromCode(code), message)
}
