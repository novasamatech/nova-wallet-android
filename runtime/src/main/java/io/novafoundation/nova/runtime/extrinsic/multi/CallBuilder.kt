package io.novafoundation.nova.runtime.extrinsic.multi

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module

interface CallBuilder {

    val runtime: RuntimeSnapshot

    val calls: List<GenericCall.Instance>

    fun addCall(
        moduleName: String,
        callName: String,
        arguments: Map<String, Any?>
    ): CallBuilder
}

class SimpleCallBuilder(override val runtime: RuntimeSnapshot): CallBuilder {

    override val calls = mutableListOf<GenericCall.Instance>()

    override fun addCall(moduleName: String, callName: String, arguments: Map<String, Any?>): CallBuilder {
        val module = runtime.metadata.module(moduleName)
        val function = module.call(callName)

        calls.add(GenericCall.Instance(module, function, arguments))

        return this
    }
}
