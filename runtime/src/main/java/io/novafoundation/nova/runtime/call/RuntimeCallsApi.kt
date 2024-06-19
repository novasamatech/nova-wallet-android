package io.novafoundation.nova.runtime.call

import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.runtime.network.rpc.StateCallRequest
import io.novafoundation.nova.runtime.network.rpc.stateCall
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.registry.getOrThrow
import io.novasama.substrate_sdk_android.runtime.definitions.types.bytes
import io.novasama.substrate_sdk_android.wsrpc.SocketService

typealias RuntimeTypeName = String
typealias RuntimeTypeValue = Any?

interface RuntimeCallsApi {

    val runtime: RuntimeSnapshot

    // TODO we can do better than that - it is possible to auto-detect method signature's types
    // However it requires a separate research
    // We should revisit this when Metadata v15 will take place
    /**
     * @param arguments - list of pairs [runtimeTypeValue, runtimeTypeName],
     * where runtimeTypeValue is value to be encoded and runtimeTypeName is type name that can be found in [TypeRegistry]
     * It can also be null, in that case argument is considered as already encoded in hex form
     */
    suspend fun <R> call(
        section: String,
        method: String,
        arguments: List<Pair<RuntimeTypeValue, RuntimeTypeName?>>,
        returnType: RuntimeTypeName,
        returnBinding: (Any?) -> R
    ): R
}

internal class RealRuntimeCallsApi(
    override val runtime: RuntimeSnapshot,
    private val socketService: SocketService,
) : RuntimeCallsApi {

    override suspend fun <R> call(
        section: String,
        method: String,
        arguments: List<Pair<RuntimeTypeValue, RuntimeTypeName?>>,
        returnType: String,
        returnBinding: (Any?) -> R
    ): R {
        val runtimeApiName = createRuntimeApiName(section, method)
        val data = encodeArguments(arguments)

        val request = StateCallRequest(runtimeApiName, data)
        val response = socketService.stateCall(request)

        val decoded = decodeResponse(response, returnType)

        return returnBinding(decoded)
    }

    private fun decodeResponse(responseHex: String?, returnTypeName: String): Any? {
        val returnType = runtime.typeRegistry.getOrThrow(returnTypeName)

        return responseHex?.let {
            returnType.fromHexOrIncompatible(it, runtime)
        }
    }

    private fun encodeArguments(arguments: List<Pair<RuntimeTypeValue, RuntimeTypeName?>>): String {
        return buildString {
            arguments.forEach { (typeValue, runtimeTypeName) ->
                val argument = if (runtimeTypeName != null) {
                    val type = runtime.typeRegistry.getOrThrow(runtimeTypeName)
                    val encodedArgument = type.bytes(runtime, typeValue)

                    encodedArgument.toHexString(withPrefix = false)
                } else {
                    typeValue.toString()
                }

                append(argument)
            }
        }.requireHexPrefix()
    }

    private fun createRuntimeApiName(section: String, method: String): String {
        return "${section}_$method"
    }
}
