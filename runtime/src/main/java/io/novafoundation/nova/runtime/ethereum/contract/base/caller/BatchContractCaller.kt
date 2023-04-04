package io.novafoundation.nova.runtime.ethereum.contract.base.caller

import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import io.novafoundation.nova.core.updater.callApiOrThrow
import io.novafoundation.nova.runtime.ethereum.subscribtion.BatchId
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import java.util.concurrent.CompletableFuture

class BatchContractCaller(
    private val batchId: BatchId,
    private val ethereumSharedRequestsBuilder: EthereumSharedRequestsBuilder,
) : ContractCaller {

    override fun ethCall(transaction: Transaction, defaultBlockParameter: DefaultBlockParameter): CompletableFuture<EthCall> {
        val request = ethereumSharedRequestsBuilder.callApiOrThrow.ethCall(transaction, defaultBlockParameter)

        return ethereumSharedRequestsBuilder.ethBatchRequestAsync(batchId, request)
    }
}
