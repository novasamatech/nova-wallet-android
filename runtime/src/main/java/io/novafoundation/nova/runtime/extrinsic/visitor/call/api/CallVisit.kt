package io.novafoundation.nova.runtime.extrinsic.visitor.call.api

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface CallVisit {
    /**
     * Call that is currently visiting
     */
    val call: GenericCall.Instance

    /**
     * Origin's account id that this call has been dispatched with
     */
    val callOrigin: AccountIdKey

    /**
     * The depth of currently visiting call
     */
    val depth: Int
}

class LeafCallVisit(
    override val call: GenericCall.Instance,
    override val callOrigin: AccountIdKey,
    override val depth: Int
) : CallVisit

val CallVisit.isLeaf: Boolean
    get() = this is LeafCallVisit

interface BatchCallVisit : CallVisit {

    val batchedCalls: List<GenericCall.Instance>
}

interface MultisigCallVisit : CallVisit {

    val signatory: AccountIdKey
        get() = callOrigin

    val otherSignatories: List<AccountIdKey>

    val threshold: Int

    val multisig: AccountIdKey

    val nestedCall: GenericCall.Instance
}

interface ProxyCallVisit : CallVisit {

    val proxy: AccountIdKey
        get() = callOrigin

    val proxied: AccountIdKey

    val nestedCall: GenericCall.Instance
}

fun CallVisit.requireLeafOrNull(): LeafCallVisit? {
    return this as? LeafCallVisit
}
