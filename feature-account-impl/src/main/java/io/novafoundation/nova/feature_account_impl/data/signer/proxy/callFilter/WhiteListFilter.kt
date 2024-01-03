package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class WhiteListFilter(private val matchingModule: String, private val matchingCalls: List<String>?) : CallFilter {

    constructor(matchingModule: String) : this(matchingModule, null)

    override fun canExecute(call: GenericCall.Instance): Boolean {
        val callModule = call.module.name
        val callName = call.function.name

        if (matchingModule == callModule) {
            if (matchingCalls == null) return true
            if (matchingCalls.contains(callName)) return true
        }

        return false
    }
}
