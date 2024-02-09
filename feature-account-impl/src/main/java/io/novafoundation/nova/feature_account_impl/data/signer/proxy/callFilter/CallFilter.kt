package io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

interface CallFilter {
    fun canExecute(call: GenericCall.Instance): Boolean
}
