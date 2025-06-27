package io.novafoundation.nova.feature_account_impl.data.multisig.api.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.HexString

class GetPedingMultisigOperationsResponse(val multisigOperations: SubQueryNodes<OperationRemote>) {

    class OperationRemote(val callHash: HexString, val callData: HexString?, val timestamp: Long, val events: SubQueryNodes<OperationEvent>)

    class OperationEvent(val timestamp: Long)
}
