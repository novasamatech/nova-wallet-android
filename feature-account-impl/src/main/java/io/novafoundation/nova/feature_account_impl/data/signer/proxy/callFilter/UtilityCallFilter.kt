package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import io.novafoundation.nova.common.utils.Modules
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

/**
 * Utility calls keep the origin filters when dispatching their nested calls. Checking only the
 * outer Utility call would therefore accept batches that the chain rejects on an inner call.
 */
class UtilityCallFilter(
    private val delegate: CallFilter,
) : CallFilter {

    override fun canExecute(call: GenericCall.Instance): Boolean {
        if (!delegate.canExecute(call)) return false
        if (call.module.name != Modules.UTILITY) return true

        return call.arguments.values
            .flatMap(::nestedCalls)
            .all(::canExecute)
    }

    private fun nestedCalls(value: Any?): List<GenericCall.Instance> {
        return when (value) {
            is GenericCall.Instance -> listOf(value)
            is Iterable<*> -> value.flatMap(::nestedCalls)
            is Array<*> -> value.flatMap(::nestedCalls)
            is Map<*, *> -> value.values.flatMap(::nestedCalls)
            else -> emptyList()
        }
    }
}
