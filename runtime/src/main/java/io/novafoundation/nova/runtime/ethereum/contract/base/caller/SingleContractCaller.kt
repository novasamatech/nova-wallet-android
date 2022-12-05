package io.novafoundation.nova.runtime.ethereum.contract.base.caller

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import java.util.concurrent.CompletableFuture

class SingleContractCaller(
    private val web3j: Web3j
) : ContractCaller {

    override fun ethCall(transaction: Transaction, defaultBlockParameter: DefaultBlockParameter): CompletableFuture<EthCall> {
        val request = web3j.ethCall(transaction, defaultBlockParameter)

        return request.sendAsync()
    }
}
