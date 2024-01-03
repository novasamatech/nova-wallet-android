package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class EverythingFilter : CallFilter {

    override fun canExecute(call: GenericCall.Instance): Boolean {
        return true
    }
}
