package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class CompoundCallFilter(
    private val filters: List<CallFilter>
) : CallFilter {

    constructor(vararg filters: CallFilter) : this(filters.toList())

    override fun canExecute(call: GenericCall.Instance): Boolean {
        return filters.all { it.canExecute(call) }
    }
}
