package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType.DELAYED
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType.IMMEDIATE

/**
 * Specifies whether the actual transaction call (e.g. transfer) will be executed immediately or delayed
 */
enum class CallExecutionType {

    /**
     * Actual call is executed immediately, together with the transaction itself
     * This is the most common case
     */
    IMMEDIATE,

    /**
     * Actual call's executed is delayed - transaction only executes preparation step
     * Examples: multisig or delayed proxies operations
     */
    DELAYED
}

fun CallExecutionType.isImmediate(): Boolean {
    return this == IMMEDIATE
}

fun CallExecutionType.intersect(other: CallExecutionType) : CallExecutionType {
    return if (isImmediate() && other.isImmediate()) {
        IMMEDIATE
    } else {
        DELAYED
    }
}
