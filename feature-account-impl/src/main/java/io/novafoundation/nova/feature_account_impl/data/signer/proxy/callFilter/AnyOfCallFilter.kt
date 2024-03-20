package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class AnyOfCallFilter(
    private val filters: List<CallFilter>
) : CallFilter {

    constructor(vararg filters: CallFilter) : this(filters.toList())

    override fun canExecute(call: GenericCall.Instance): Boolean {
        return filters.any { it.canExecute(call) }
    }
}
