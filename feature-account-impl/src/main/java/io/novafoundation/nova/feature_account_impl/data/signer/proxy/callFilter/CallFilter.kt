package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface CallFilter {
    fun canExecute(call: GenericCall.Instance): Boolean
}
