package io.novafoundation.nova.feature_account_impl.data.multisig.api.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.HexString

class GetCallDatasResponse(val multisigOperations: SubQueryNodes<CallDataRemote>) {

    class CallDataRemote(val callHash: HexString, val callData: HexString?)
}
