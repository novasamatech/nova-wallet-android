package io.novafoundation.nova.common.data.network.ethereum.contract.base.caller

import io.novafoundation.nova.common.data.network.ethereum.subscribtion.BatchId
import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import kotlinx.coroutines.Deferred
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall

class BatchContractCaller(
    private val batchId: BatchId,
    private val ethereumSharedRequestsBuilder: EthereumSharedRequestsBuilder,
): ContractCaller {

    override fun ethCall(transaction: Transaction, defaultBlockParameter: DefaultBlockParameter): Deferred<EthCall> {
        val request = ethereumSharedRequestsBuilder.web3Api.ethCall(transaction, defaultBlockParameter)

        return ethereumSharedRequestsBuilder.ethBatchRequestAsync(batchId, request)
    }
}
